package scenario

import (
	"context"
	"fmt"
	"os"
	"time"

	"github.com/minekube/craftwright/internal/engine"
	"gopkg.in/yaml.v3"
)

type File struct {
	Version int               `yaml:"version"`
	Clients map[string]Client `yaml:"clients"`
	Steps   []Step            `yaml:"steps"`
}

type Client struct {
	MC      string `yaml:"mc"`
	Loader  string `yaml:"loader"`
	Offline bool   `yaml:"offline"`
}

type Step struct {
	Launch  string      `yaml:"launch"`
	Connect ConnectStep `yaml:"connect"`
	Chat    ChatStep    `yaml:"chat"`
	Wait    WaitStep    `yaml:"wait"`
}

type ConnectStep struct {
	Client string `yaml:"client"`
	Server string `yaml:"server"`
}

type ChatStep struct {
	Client  string `yaml:"client"`
	Message string `yaml:"message"`
}

type WaitStep struct {
	Client  string `yaml:"client"`
	Chat    string `yaml:"chat"`
	Timeout string `yaml:"timeout"`
}

type Result struct {
	OK    bool `json:"ok"`
	Steps int  `json:"steps"`
}

func ValidateFile(path string) (Result, error) {
	file, err := loadFile(path)
	if err != nil {
		return Result{}, err
	}
	if err := validate(file); err != nil {
		return Result{}, err
	}
	return Result{OK: true, Steps: len(file.Steps)}, nil
}

func RunFile(ctx context.Context, eng engine.Engine, path string) (Result, error) {
	file, err := loadFile(path)
	if err != nil {
		return Result{}, err
	}
	if err := validate(file); err != nil {
		return Result{}, err
	}

	var steps int
	for i, step := range file.Steps {
		switch {
		case step.Launch != "":
			client, ok := file.Clients[step.Launch]
			if !ok {
				return Result{}, fmt.Errorf("step %d: client %s not defined", i+1, step.Launch)
			}
			if _, err := eng.Launch(ctx, engine.LaunchRequest{
				Name:             step.Launch,
				MinecraftVersion: client.MC,
				Loader:           client.Loader,
				Offline:          client.Offline,
			}); err != nil {
				return Result{}, fmt.Errorf("step %d launch: %w", i+1, err)
			}
		case step.Connect.Client != "" || step.Connect.Server != "":
			if err := eng.Connect(ctx, step.Connect.Client, step.Connect.Server); err != nil {
				return Result{}, fmt.Errorf("step %d connect: %w", i+1, err)
			}
		case step.Chat.Client != "" || step.Chat.Message != "":
			if err := eng.Chat(ctx, step.Chat.Client, step.Chat.Message); err != nil {
				return Result{}, fmt.Errorf("step %d chat: %w", i+1, err)
			}
		case step.Wait.Client != "" || step.Wait.Chat != "" || step.Wait.Timeout != "":
			timeout, err := waitTimeout(step.Wait)
			if err != nil {
				return Result{}, fmt.Errorf("step %d wait: %w", i+1, err)
			}
			if _, err := eng.Wait(ctx, engine.WaitRequest{
				Client:      step.Wait.Client,
				ChatPattern: step.Wait.Chat,
				Timeout:     timeout,
			}); err != nil {
				return Result{}, fmt.Errorf("step %d wait: %w", i+1, err)
			}
		default:
			return Result{}, fmt.Errorf("step %d: empty step", i+1)
		}
		steps++
	}

	return Result{OK: true, Steps: steps}, nil
}

func loadFile(path string) (File, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return File{}, err
	}
	var file File
	if err := yaml.Unmarshal(data, &file); err != nil {
		return File{}, err
	}
	return file, nil
}

func validate(file File) error {
	for i, step := range file.Steps {
		if step.Wait.Client != "" || step.Wait.Chat != "" || step.Wait.Timeout != "" {
			if _, err := waitTimeout(step.Wait); err != nil {
				return fmt.Errorf("step %d wait: %w", i+1, err)
			}
		}
	}
	return nil
}

func waitTimeout(step WaitStep) (time.Duration, error) {
	if step.Timeout == "" {
		return 30 * time.Second, nil
	}
	return time.ParseDuration(step.Timeout)
}
