package auth

import (
	"testing"

	"github.com/zalando/go-keyring"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

func TestKeyringStore_RoundTrip(t *testing.T) {
	keyring.MockInit()
	store := keyringStore{}
	server := "https://tissue.example.com"
	want := domain.TokenPair{Access: "a1", Refresh: "r1"}

	if err := store.Save(server, want); err != nil {
		t.Fatalf("save: %v", err)
	}

	got, ok, err := store.Load(server)
	if err != nil || !ok {
		t.Fatalf("load: ok=%v err=%v", ok, err)
	}
	if got != want {
		t.Fatalf("got %+v, want %+v", got, want)
	}

	if err := store.Clear(server); err != nil {
		t.Fatalf("clear: %v", err)
	}
	if _, ok, _ := store.Load(server); ok {
		t.Fatal("tokens still present after clear")
	}
}

func TestKeyringStore_LoadMissing(t *testing.T) {
	keyring.MockInit()

	_, ok, err := keyringStore{}.Load("https://none.example.com")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if ok {
		t.Fatal("expected not found")
	}
}
