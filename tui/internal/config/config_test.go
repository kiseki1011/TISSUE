package config

import "testing"

// loadTemp points config at a throwaway dir so tests never touch the real config file.
func loadTemp(t *testing.T) *Config {
	t.Helper()
	tmp := t.TempDir()
	t.Setenv("HOME", tmp)            // os.UserConfigDir on darwin
	t.Setenv("XDG_CONFIG_HOME", tmp) // os.UserConfigDir on linux
	cfg, err := Load()
	if err != nil {
		t.Fatalf("load config: %v", err)
	}
	return cfg
}

func TestLastProjectPersists(t *testing.T) {
	cfg := loadTemp(t)
	const server = "http://localhost:8080"

	if got := cfg.LastProjectFor(server); got != "" {
		t.Fatalf("a fresh config should have no last project, got %q", got)
	}
	if err := cfg.SetLastProject(server, "ENG"); err != nil {
		t.Fatalf("set: %v", err)
	}

	reloaded := reload(t)
	if got := reloaded.LastProjectFor(server); got != "ENG" {
		t.Errorf("last project not persisted: %q, want ENG", got)
	}

	if err := reloaded.SetLastProject(server, ""); err != nil {
		t.Fatalf("clear: %v", err)
	}
	if got := reload(t).LastProjectFor(server); got != "" {
		t.Errorf("cleared last project must not survive a reload, got %q", got)
	}
}

func TestLastProjectPerServer(t *testing.T) {
	cfg := loadTemp(t)
	if err := cfg.SetLastProject("http://a", "ONE"); err != nil {
		t.Fatalf("set a: %v", err)
	}
	if err := cfg.SetLastProject("http://b", "TWO"); err != nil {
		t.Fatalf("set b: %v", err)
	}
	r := reload(t)
	if got := r.LastProjectFor("http://a"); got != "ONE" {
		t.Errorf("server a = %q, want ONE", got)
	}
	if got := r.LastProjectFor("http://b"); got != "TWO" {
		t.Errorf("server b = %q, want TWO", got)
	}
}

func TestLastProjectSkipsRedundantWrite(t *testing.T) {
	cfg := loadTemp(t)
	const server = "http://localhost:8080"
	if err := cfg.SetLastProject(server, ""); err != nil {
		t.Fatalf("clear-when-clear: %v", err)
	}
	if reload(t).LastProject != nil {
		t.Error("clearing an already-clear pointer should not create the map on disk")
	}
	if err := cfg.SetLastProject(server, "ENG"); err != nil {
		t.Fatalf("set: %v", err)
	}
	if err := cfg.SetLastProject(server, "ENG"); err != nil {
		t.Fatalf("re-set same: %v", err)
	}
	if got := reload(t).LastProjectFor(server); got != "ENG" {
		t.Errorf("re-setting the same key = %q, want ENG", got)
	}
}

func TestProjectFilterPersists(t *testing.T) {
	cfg := loadTemp(t)
	if _, ok := cfg.ProjectFilter("http://a", "ENG"); ok {
		t.Fatal("a fresh config should have no saved filter")
	}
	if err := cfg.SetProjectFilter("http://a", "ENG", FilterState{Priorities: []string{"P0"}, AssigneeMe: true}); err != nil {
		t.Fatalf("set: %v", err)
	}
	if err := cfg.SetProjectFilter("http://a", "OPS", FilterState{StateCategories: []string{"COMPLETED"}}); err != nil {
		t.Fatalf("set OPS: %v", err)
	}
	if err := cfg.SetProjectFilter("http://b", "ENG", FilterState{Priorities: []string{"P4"}}); err != nil {
		t.Fatalf("set server b: %v", err)
	}

	r := reload(t)
	eng, ok := r.ProjectFilter("http://a", "ENG")
	if !ok || len(eng.Priorities) != 1 || eng.Priorities[0] != "P0" || !eng.AssigneeMe {
		t.Errorf("server a/ENG not persisted: %+v ok=%v", eng, ok)
	}
	if ops, _ := r.ProjectFilter("http://a", "OPS"); len(ops.StateCategories) != 1 || ops.StateCategories[0] != "COMPLETED" {
		t.Errorf("per-project isolation broken: %+v", ops)
	}
	if b, _ := r.ProjectFilter("http://b", "ENG"); len(b.Priorities) != 1 || b.Priorities[0] != "P4" {
		t.Errorf("per-server isolation broken: %+v", b)
	}
}

func TestProjectFilterEmptyIsDistinctFromAbsent(t *testing.T) {
	cfg := loadTemp(t)
	if err := cfg.SetProjectFilter("http://a", "ENG", FilterState{}); err != nil {
		t.Fatalf("set empty: %v", err)
	}
	r := reload(t)
	if _, ok := r.ProjectFilter("http://a", "ENG"); !ok {
		t.Error("a stored empty filter must reload as a present entry (show everything), not absent")
	}
	if _, ok := r.ProjectFilter("http://a", "NOPE"); ok {
		t.Error("an unsaved project must report no entry")
	}
}

func reload(t *testing.T) *Config {
	t.Helper()
	cfg, err := Load()
	if err != nil {
		t.Fatalf("reload config: %v", err)
	}
	return cfg
}
