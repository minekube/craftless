package cli

import (
	"encoding/json"
	"errors"
	"fmt"
	"io"
)

var ErrInvalidUsage = errors.New("invalid usage")

type appError struct {
	Code int
	Err  error
}

func (e appError) Error() string {
	return e.Err.Error()
}

func (e appError) Unwrap() error {
	return e.Err
}

func usageError(format string, args ...any) error {
	return appError{
		Code: 2,
		Err:  fmt.Errorf("%w: %s", ErrInvalidUsage, fmt.Sprintf(format, args...)),
	}
}

func (opts GlobalOptions) Validate() error {
	if opts.JSONL {
		return usageError("--jsonl is reserved until streaming JSON Lines output is implemented")
	}
	modes := 0
	if opts.JSON {
		modes++
	}
	if opts.JSONL {
		modes++
	}
	if opts.Plain {
		modes++
	}
	if modes > 1 {
		return usageError("choose only one of --json, --jsonl, or --plain")
	}
	return nil
}

func WriteJSON(w io.Writer, v any) error {
	enc := json.NewEncoder(w)
	enc.SetEscapeHTML(false)
	return enc.Encode(v)
}

func WriteJSONError(w io.Writer, err error, code int) error {
	return WriteJSON(w, map[string]any{
		"ok": false,
		"error": map[string]any{
			"code":    code,
			"message": err.Error(),
		},
	})
}

func exitCode(err error) int {
	var appErr appError
	if errors.As(err, &appErr) {
		return appErr.Code
	}
	return 1
}
