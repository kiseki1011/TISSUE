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
	// the caller's team (admin-assigned) and position (self-service) — ids are 0 and names "" when unset
	TeamID       int
	TeamName     string
	PositionID   int
	PositionName string
}

// IsAdmin reports whether the member may manage global catalogs (ADMIN or SUPER_ADMIN).
func (p Profile) IsAdmin() bool {
	return p.Role == "ADMIN" || p.Role == "SUPER_ADMIN"
}

// Profile requires the authed client.
func (s *AuthService) Profile(ctx context.Context) (Profile, error) {
	resp, err := s.api.GetMyProfileWithResponse(ctx)
	if err != nil {
		return Profile{}, fmt.Errorf("get profile: %w", err)
	}
	if resp.JSON200 == nil {
		return Profile{}, newAPIError(resp.StatusCode(), resp.Body)
	}
	p := resp.JSON200
	role := ""
	if p.Role != nil {
		role = string(*p.Role)
	}
	prof := Profile{
		Name:     deref(p.Name),
		Username: deref(p.Username),
		Email:    deref(p.Email),
		Role:     role,
	}
	if p.Team != nil {
		prof.TeamID, prof.TeamName = derefInt64(p.Team.Id), deref(p.Team.Name)
	}
	if p.Position != nil {
		prof.PositionID, prof.PositionName = derefInt64(p.Position.Id), deref(p.Position.Name)
	}
	return prof, nil
}
