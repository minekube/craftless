package engine

import (
	"context"
	"time"
)

type State string

const (
	StateRunning   State = "running"
	StateConnected State = "connected"
	StateStopped   State = "stopped"
)

type EventType string

const (
	EventChat  EventType = "client.chat"
	EventState EventType = "client.state"
)

type Client struct {
	Name             string `json:"name"`
	State            State  `json:"state"`
	MinecraftVersion string `json:"minecraftVersion"`
	Loader           string `json:"loader"`
	Offline          bool   `json:"offline"`
	Server           string `json:"server,omitempty"`
}

type Event struct {
	Type    EventType `json:"type"`
	Client  string    `json:"client"`
	Message string    `json:"message,omitempty"`
	State   State     `json:"state,omitempty"`
}

type LaunchRequest struct {
	Name             string
	MinecraftVersion string
	Loader           string
	Offline          bool
	Username         string
	Server           string
	Timeout          time.Duration
	ArtifactsDir     string
}

type WaitRequest struct {
	Client      string
	ChatPattern string
	Timeout     time.Duration
}

type Engine interface {
	Launch(context.Context, LaunchRequest) (Client, error)
	List(context.Context) ([]Client, error)
	Status(context.Context, string) (Client, error)
	Connect(context.Context, string, string) error
	Chat(context.Context, string, string) error
	Wait(context.Context, WaitRequest) (Event, error)
	Logs(context.Context, string) ([]string, error)
	Stop(context.Context, string, bool) error
}
