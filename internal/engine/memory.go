package engine

type memoryEngine struct{}

func NewMemory() Engine {
	return memoryEngine{}
}
