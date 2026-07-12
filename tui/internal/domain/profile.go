package domain

import (
	"context"
	"fmt"
)

// Profile is the authenticated member's identity shown in the app chrome.
type Profile struct {
	Name     string
	Username string
	Email    string
	Role     string // system role: USER, ADMIN, or SUPER_ADMIN
}

// IsAdmin reports whether the member may manage global catalogs (ADMIN or SUPER_ADMIN).
func (p Profile) IsAdmin() bool {
	return p.Role == "ADMIN" || p.Role == "SUPER_ADMIN"
}

// Profile reads the current member's profile. It requires the authed client.
func (s *AuthService) Profile(ctx context.Context) (Profile, error) {
	resp, err := s.api.GetMyProfileWithResponse(ctx)
	if err != nil {
		return Profile{}, fmt.Errorf("get profile: %w", err)
	}
	if resp.JSON200 == nil {
		return Profile{}, &APIError{Status: resp.StatusCode()}
	}
	p := resp.JSON200
	role := ""
	if p.Role != nil {
		role = string(*p.Role)
	}
	return Profile{
		Name:     deref(p.Name),
		Username: deref(p.Username),
		Email:    deref(p.Email),
		Role:     role,
	}, nil
}
