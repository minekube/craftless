package engine

import (
	"context"
	"fmt"
	"regexp"
	"sort"
	"sync"
	"time"
)

type memoryEngine struct {
	mu      sync.Mutex
	cond    *sync.Cond
	clients map[string]Client
	events  []Event
	logs    map[string][]string
}

func NewMemory() Engine {
	m := &memoryEngine{
		clients: map[string]Client{},
		logs:    map[string][]string{},
	}
	m.cond = sync.NewCond(&m.mu)
	return m
}

func (m *memoryEngine) Launch(ctx context.Context, req LaunchRequest) (Client, error) {
	if req.Name == "" {
		return Client{}, fmt.Errorf("client name is required")
	}
	if req.MinecraftVersion == "" {
		return Client{}, fmt.Errorf("minecraft version is required")
	}
	if req.Loader == "" {
		return Client{}, fmt.Errorf("loader is required")
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	if existing, ok := m.clients[req.Name]; ok && existing.State != StateStopped {
		return Client{}, fmt.Errorf("client %s is already running", req.Name)
	}
	client := Client{Name: req.Name, State: StateRunning, MinecraftVersion: req.MinecraftVersion, Loader: req.Loader, Offline: req.Offline, Server: req.Server}
	m.clients[req.Name] = client
	m.appendEventLocked(Event{Type: EventState, Client: req.Name, State: StateRunning})
	m.logs[req.Name] = append(m.logs[req.Name], "LAUNCH "+req.MinecraftVersion+" "+req.Loader)
	if req.Server != "" {
		client.State = StateConnected
		client.Server = req.Server
		m.clients[req.Name] = client
		m.appendEventLocked(Event{Type: EventState, Client: req.Name, State: StateConnected})
		m.logs[req.Name] = append(m.logs[req.Name], "CONNECT "+req.Server)
	}
	return client, nil
}

func (m *memoryEngine) List(ctx context.Context) ([]Client, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	out := make([]Client, 0, len(m.clients))
	for _, client := range m.clients {
		out = append(out, client)
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].Name < out[j].Name
	})
	return out, nil
}

func (m *memoryEngine) Status(ctx context.Context, name string) (Client, error) {
	if name == "" {
		return Client{}, fmt.Errorf("client name is required")
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	client, ok := m.clients[name]
	if !ok {
		return Client{}, fmt.Errorf("client %s not found", name)
	}
	return client, nil
}

func (m *memoryEngine) Connect(ctx context.Context, name string, server string) error {
	if name == "" {
		return fmt.Errorf("client name is required")
	}
	if server == "" {
		return fmt.Errorf("server is required")
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	client, ok := m.clients[name]
	if !ok {
		return fmt.Errorf("client %s not found", name)
	}
	if client.State == StateStopped {
		return fmt.Errorf("client %s is stopped", name)
	}
	client.State = StateConnected
	client.Server = server
	m.clients[name] = client
	m.appendEventLocked(Event{Type: EventState, Client: name, State: StateConnected})
	m.logs[name] = append(m.logs[name], "CONNECT "+server)
	return nil
}

func (m *memoryEngine) Chat(ctx context.Context, name string, message string) error {
	if name == "" {
		return fmt.Errorf("client name is required")
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	client, ok := m.clients[name]
	if !ok {
		return fmt.Errorf("client %s not found", name)
	}
	if client.State == StateStopped {
		return fmt.Errorf("client %s is stopped", name)
	}
	m.appendEventLocked(Event{Type: EventChat, Client: name, Message: message})
	m.logs[name] = append(m.logs[name], "CHAT "+message)
	return nil
}

func (m *memoryEngine) Wait(ctx context.Context, req WaitRequest) (Event, error) {
	if req.Client == "" {
		return Event{}, fmt.Errorf("client name is required")
	}
	if req.ChatPattern == "" {
		return Event{}, fmt.Errorf("chat pattern is required")
	}
	if err := ctx.Err(); err != nil {
		return Event{}, err
	}
	pattern := req.ChatPattern
	if len(pattern) >= 2 && pattern[0] == '/' && pattern[len(pattern)-1] == '/' {
		pattern = pattern[1 : len(pattern)-1]
	}
	re, err := regexp.Compile(pattern)
	if err != nil {
		return Event{}, err
	}

	var deadline time.Time
	if req.Timeout > 0 {
		deadline = time.Now().Add(req.Timeout)
	}

	m.mu.Lock()
	defer m.mu.Unlock()
	if _, ok := m.clients[req.Client]; !ok {
		return Event{}, fmt.Errorf("client %s not found", req.Client)
	}

	var timer *time.Timer
	if !deadline.IsZero() {
		timer = time.AfterFunc(time.Until(deadline), func() {
			m.mu.Lock()
			m.cond.Broadcast()
			m.mu.Unlock()
		})
		defer timer.Stop()
	}

	done := make(chan struct{})
	if ctx.Done() != nil {
		defer close(done)
		go func() {
			select {
			case <-ctx.Done():
				m.mu.Lock()
				m.cond.Broadcast()
				m.mu.Unlock()
			case <-done:
			}
		}()
	}

	for {
		for _, event := range m.events {
			if event.Client == req.Client && event.Type == EventChat && re.MatchString(event.Message) {
				return event, nil
			}
		}
		if err := ctx.Err(); err != nil {
			return Event{}, err
		}
		if !deadline.IsZero() && !time.Now().Before(deadline) {
			return Event{}, fmt.Errorf("timed out waiting for chat %q", req.ChatPattern)
		}
		m.cond.Wait()
	}
}

func (m *memoryEngine) Logs(ctx context.Context, name string) ([]string, error) {
	if name == "" {
		return nil, fmt.Errorf("client name is required")
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	if _, ok := m.clients[name]; !ok {
		return nil, fmt.Errorf("client %s not found", name)
	}
	out := append([]string(nil), m.logs[name]...)
	return out, nil
}

func (m *memoryEngine) Stop(ctx context.Context, name string, force bool) error {
	if name == "" {
		return fmt.Errorf("client name is required")
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	client, ok := m.clients[name]
	if !ok {
		return nil
	}
	client.State = StateStopped
	m.clients[name] = client
	m.appendEventLocked(Event{Type: EventState, Client: name, State: StateStopped})
	m.logs[name] = append(m.logs[name], "STOP")
	return nil
}

func (m *memoryEngine) appendEventLocked(event Event) {
	m.events = append(m.events, event)
	m.cond.Broadcast()
}
