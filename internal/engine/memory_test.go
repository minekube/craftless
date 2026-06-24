package engine_test

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/minekube/craftwright/internal/engine"
)

func TestMemoryEngineLaunchConnectChatWaitStop(t *testing.T) {
	ctx := context.Background()
	e := engine.NewMemory()
	client, err := e.Launch(ctx, engine.LaunchRequest{Name: "alice", MinecraftVersion: "1.21.6", Loader: "fabric", Offline: true})
	if err != nil {
		t.Fatal(err)
	}
	if client.Name != "alice" || client.State != engine.StateRunning {
		t.Fatalf("client = %#v", client)
	}
	if err := e.Connect(ctx, "alice", "localhost:25565"); err != nil {
		t.Fatal(err)
	}
	if err := e.Chat(ctx, "alice", "hello"); err != nil {
		t.Fatal(err)
	}
	event, err := e.Wait(ctx, engine.WaitRequest{Client: "alice", ChatPattern: "hello", Timeout: time.Second})
	if err != nil {
		t.Fatal(err)
	}
	if event.Type != engine.EventChat || event.Message != "hello" {
		t.Fatalf("event = %#v", event)
	}
	logs, err := e.Logs(ctx, "alice")
	if err != nil {
		t.Fatal(err)
	}
	if len(logs) == 0 || logs[len(logs)-1] != "CHAT hello" {
		t.Fatalf("logs = %#v", logs)
	}
	if err := e.Stop(ctx, "alice", false); err != nil {
		t.Fatal(err)
	}
	stopped, err := e.Status(ctx, "alice")
	if err != nil {
		t.Fatal(err)
	}
	if stopped.State != engine.StateStopped {
		t.Fatalf("state = %s", stopped.State)
	}
}

func TestMemoryEngineRejectsDuplicateRunningClient(t *testing.T) {
	ctx := context.Background()
	e := engine.NewMemory()
	_, err := e.Launch(ctx, engine.LaunchRequest{Name: "alice", MinecraftVersion: "1.21.6", Loader: "fabric", Offline: true})
	if err != nil {
		t.Fatal(err)
	}
	_, err = e.Launch(ctx, engine.LaunchRequest{Name: "alice", MinecraftVersion: "1.21.6", Loader: "fabric", Offline: true})
	if err == nil {
		t.Fatal("expected duplicate launch error")
	}
}

func TestMemoryEngineWaitReturnsFutureChatEvent(t *testing.T) {
	ctx := context.Background()
	e := engine.NewMemory()
	_, err := e.Launch(ctx, engine.LaunchRequest{Name: "alice", MinecraftVersion: "1.21.6", Loader: "fabric", Offline: true})
	if err != nil {
		t.Fatal(err)
	}

	type waitResult struct {
		event engine.Event
		err   error
	}
	waited := make(chan waitResult, 1)
	go func() {
		event, err := e.Wait(ctx, engine.WaitRequest{Client: "alice", ChatPattern: "/Welcome/", Timeout: time.Second})
		waited <- waitResult{event: event, err: err}
	}()

	time.Sleep(25 * time.Millisecond)
	select {
	case result := <-waited:
		t.Fatalf("Wait returned before matching chat: event=%#v err=%v", result.event, result.err)
	default:
	}

	if err := e.Chat(ctx, "alice", "Welcome alice"); err != nil {
		t.Fatal(err)
	}
	select {
	case result := <-waited:
		if result.err != nil {
			t.Fatal(result.err)
		}
		if result.event.Type != engine.EventChat || result.event.Message != "Welcome alice" {
			t.Fatalf("event = %#v", result.event)
		}
	case <-time.After(500 * time.Millisecond):
		t.Fatal("Wait did not return matching chat event")
	}
}

func TestMemoryEngineWaitHonorsTimeout(t *testing.T) {
	ctx := context.Background()
	e := engine.NewMemory()
	_, err := e.Launch(ctx, engine.LaunchRequest{Name: "alice", MinecraftVersion: "1.21.6", Loader: "fabric", Offline: true})
	if err != nil {
		t.Fatal(err)
	}

	timeout := 75 * time.Millisecond
	start := time.Now()
	_, err = e.Wait(ctx, engine.WaitRequest{Client: "alice", ChatPattern: "missing", Timeout: timeout})
	elapsed := time.Since(start)
	if err == nil {
		t.Fatal("expected timeout error")
	}
	if elapsed < 50*time.Millisecond {
		t.Fatalf("Wait returned too early: elapsed=%s timeout=%s", elapsed, timeout)
	}
}

func TestMemoryEngineWaitHonorsCanceledContext(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	cancel()
	e := engine.NewMemory()
	_, err := e.Launch(context.Background(), engine.LaunchRequest{Name: "alice", MinecraftVersion: "1.21.6", Loader: "fabric", Offline: true})
	if err != nil {
		t.Fatal(err)
	}

	_, err = e.Wait(ctx, engine.WaitRequest{Client: "alice", ChatPattern: "missing", Timeout: time.Second})
	if !errors.Is(err, context.Canceled) {
		t.Fatalf("err = %v, want context.Canceled", err)
	}
}

func TestMemoryEngineRejectsStoppedClientConnectAndChat(t *testing.T) {
	ctx := context.Background()
	e := engine.NewMemory()
	_, err := e.Launch(ctx, engine.LaunchRequest{Name: "alice", MinecraftVersion: "1.21.6", Loader: "fabric", Offline: true})
	if err != nil {
		t.Fatal(err)
	}
	if err := e.Stop(ctx, "alice", false); err != nil {
		t.Fatal(err)
	}
	if err := e.Connect(ctx, "alice", "localhost:25565"); err == nil {
		t.Fatal("expected stopped client connect error")
	}
	if err := e.Chat(ctx, "alice", "hello"); err == nil {
		t.Fatal("expected stopped client chat error")
	}
}

func TestMemoryEngineListSortsClientsByName(t *testing.T) {
	ctx := context.Background()
	e := engine.NewMemory()
	_, err := e.Launch(ctx, engine.LaunchRequest{Name: "bob", MinecraftVersion: "1.21.6", Loader: "fabric", Offline: true})
	if err != nil {
		t.Fatal(err)
	}
	_, err = e.Launch(ctx, engine.LaunchRequest{Name: "alice", MinecraftVersion: "1.21.6", Loader: "fabric", Offline: true})
	if err != nil {
		t.Fatal(err)
	}

	clients, err := e.List(ctx)
	if err != nil {
		t.Fatal(err)
	}
	if len(clients) != 2 || clients[0].Name != "alice" || clients[1].Name != "bob" {
		t.Fatalf("clients = %#v", clients)
	}
}

func TestMemoryEngineValidatesInputs(t *testing.T) {
	ctx := context.Background()

	launchCases := []struct {
		name string
		req  engine.LaunchRequest
	}{
		{name: "empty name", req: engine.LaunchRequest{MinecraftVersion: "1.21.6", Loader: "fabric", Offline: true}},
		{name: "empty minecraft version", req: engine.LaunchRequest{Name: "alice", Loader: "fabric", Offline: true}},
		{name: "empty loader", req: engine.LaunchRequest{Name: "alice", MinecraftVersion: "1.21.6", Offline: true}},
	}
	for _, tc := range launchCases {
		t.Run(tc.name, func(t *testing.T) {
			e := engine.NewMemory()
			if _, err := e.Launch(ctx, tc.req); err == nil {
				t.Fatal("expected launch validation error")
			}
		})
	}

	e := engine.NewMemory()
	_, err := e.Launch(ctx, engine.LaunchRequest{Name: "alice", MinecraftVersion: "1.21.6", Loader: "fabric", Offline: true})
	if err != nil {
		t.Fatal(err)
	}
	if err := e.Connect(ctx, "alice", ""); err == nil {
		t.Fatal("expected empty connect server error")
	}
	if _, err := e.Wait(ctx, engine.WaitRequest{Client: "alice", ChatPattern: "", Timeout: time.Millisecond}); err == nil {
		t.Fatal("expected empty wait pattern error")
	}
	if _, err := e.Status(ctx, ""); err == nil {
		t.Fatal("expected empty status name error")
	}
	if _, err := e.Logs(ctx, ""); err == nil {
		t.Fatal("expected empty logs name error")
	}
	if err := e.Stop(ctx, "", false); err == nil {
		t.Fatal("expected empty stop name error")
	}
}
