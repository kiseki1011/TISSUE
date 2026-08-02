// Command specprep rewrites the backend OpenAPI spec into a form oapi-codegen can consume.
// It repairs the JsonNullable<T> wrapper schemas, which springdoc emits with only a {present}
// field, dropping the value. Each JsonNullable<T> reference is replaced inline with the underlying
// type marked nullable, so codegen (with nullable-type) produces a proper tri-state field.
package main

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"strings"
)

// `jsonNullableUnderlying` maps each broken wrapper schema to the value schema it should have carried.
// The enum ones fall back to a plain string. Their value type was dropped by springdoc and
// no named enum schema survives to reference, so the backend validates the enum instead of the client.
var jsonNullableUnderlying = map[string]map[string]any{
	"JsonNullableString":            {"type": "string"},
	"JsonNullableBoolean":           {"type": "boolean"},
	"JsonNullableInstant":           {"type": "string", "format": "date-time"},
	"JsonNullableColorType":         {"type": "string"},
	"JsonNullableIconType":          {"type": "string"},
	"JsonNullableIssuePriority":     {"type": "string"},
	"JsonNullableProjectVisibility": {"type": "string"},
}

func main() {
	if len(os.Args) != 3 {
		fmt.Fprintln(os.Stderr, "usage: specprep <in.json> <out.json>")
		os.Exit(2)
	}
	if err := run(os.Args[1], os.Args[2]); err != nil {
		fmt.Fprintln(os.Stderr, "specprep:", err)
		os.Exit(1)
	}
}

func run(in, out string) error {
	data, err := os.ReadFile(in)
	if err != nil {
		return fmt.Errorf("read %s: %w", in, err)
	}

	var spec map[string]any
	if err := json.Unmarshal(data, &spec); err != nil {
		return fmt.Errorf("parse %s: %w", in, err)
	}

	spec["openapi"] = "3.0.3"
	replaceJsonNullableRefs(spec)
	deleteSchemas(spec, jsonNullableNames())

	result, err := json.MarshalIndent(spec, "", "  ")
	if err != nil {
		return fmt.Errorf("encode: %w", err)
	}
	if err := os.MkdirAll(filepath.Dir(out), 0o755); err != nil {
		return fmt.Errorf("create out dir: %w", err)
	}
	if err := os.WriteFile(out, result, 0o644); err != nil {
		return fmt.Errorf("write %s: %w", out, err)
	}
	return nil
}

// `replaceJsonNullableRefs` rewrites every `{"$ref": ".../JsonNullableX"}` node in place into its nullable value schema.
func replaceJsonNullableRefs(node any) {
	switch n := node.(type) {
	case map[string]any:
		if ref, ok := n["$ref"].(string); ok {
			if under, hit := jsonNullableUnderlying[schemaName(ref)]; hit {
				for k := range n {
					delete(n, k)
				}
				for k, v := range under {
					n[k] = v
				}
				n["nullable"] = true
				return
			}
		}
		for _, v := range n {
			replaceJsonNullableRefs(v)
		}
	case []any:
		for _, v := range n {
			replaceJsonNullableRefs(v)
		}
	}
}

func deleteSchemas(spec map[string]any, names []string) {
	comps, ok := spec["components"].(map[string]any)
	if !ok {
		return
	}
	schemas, ok := comps["schemas"].(map[string]any)
	if !ok {
		return
	}
	for _, name := range names {
		delete(schemas, name)
	}
}

func jsonNullableNames() []string {
	names := make([]string, 0, len(jsonNullableUnderlying))
	for name := range jsonNullableUnderlying {
		names = append(names, name)
	}
	return names
}

func schemaName(ref string) string {
	return ref[strings.LastIndex(ref, "/")+1:]
}
