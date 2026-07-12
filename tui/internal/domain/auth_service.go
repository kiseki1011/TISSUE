package domain

import (
	"context"
	"errors"
	"fmt"
	"net/http"
	"strings"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// ErrInvalidCredentials is returned by Login when the server rejects the identifier and password.
var ErrInvalidCredentials = errors.New("invalid credentials")

// ErrIncompleteResponse is returned when a 200 response is missing tokens the caller needs.
var ErrIncompleteResponse = errors.New("server returned an incomplete response")

// APIError is a non-success HTTP response. The UI reads Status to tell rate limiting (429) apart
// from server errors.
type APIError struct {
	Status int
}

func (e *APIError) Error() string {
	return fmt.Sprintf("server returned status %d", e.Status)
}

// AuthService performs authentication calls against the API.
type AuthService struct {
	api *client.ClientWithResponses
}

// NewAuthService wraps the generated client.
func NewAuthService(api *client.ClientWithResponses) *AuthService {
	return &AuthService{api: api}
}

// SystemInfo reads the server's public configuration. It does not need credentials.
func (s *AuthService) SystemInfo(ctx context.Context) (SystemInfo, error) {
	resp, err := s.api.GetSystemInfoWithResponse(ctx)
	if err != nil {
		return SystemInfo{}, fmt.Errorf("get system info: %w", err)
	}
	if resp.JSON200 == nil {
		return SystemInfo{}, &APIError{Status: resp.StatusCode()}
	}
	return toSystemInfo(resp.JSON200), nil
}

func (s *AuthService) Login(ctx context.Context, identifier, password string) (TokenPair, error) {
	resp, err := s.api.LoginWithResponse(ctx, client.LoginJSONRequestBody{
		Identifier: identifier,
		Password:   password,
	})
	if err != nil {
		return TokenPair{}, fmt.Errorf("login: %w", err)
	}
	if resp.StatusCode() == http.StatusUnauthorized {
		return TokenPair{}, ErrInvalidCredentials
	}
	if resp.JSON200 == nil {
		return TokenPair{}, &APIError{Status: resp.StatusCode()}
	}
	return toTokenPair(resp.JSON200.AccessToken, resp.JSON200.RefreshToken)
}

// Refresh uses the current refresh token to get a new token pair.
func (s *AuthService) Refresh(ctx context.Context, refreshToken string) (TokenPair, error) {
	resp, err := s.api.RefreshTokenWithResponse(ctx, client.RefreshTokenJSONRequestBody{
		RefreshToken: refreshToken,
	})
	if err != nil {
		return TokenPair{}, fmt.Errorf("refresh token: %w", err)
	}
	if resp.JSON200 == nil {
		return TokenPair{}, &APIError{Status: resp.StatusCode()}
	}
	return toTokenPair(resp.JSON200.AccessToken, resp.JSON200.RefreshToken)
}

// StartDeviceLogin begins OIDC device flow.
func (s *AuthService) StartDeviceLogin(ctx context.Context) (DeviceAuth, error) {
	resp, err := s.api.StartOidcDeviceLoginWithResponse(ctx)
	if err != nil {
		return DeviceAuth{}, fmt.Errorf("start device login: %w", err)
	}
	if resp.JSON200 == nil {
		return DeviceAuth{}, &APIError{Status: resp.StatusCode()}
	}
	d := resp.JSON200
	return DeviceAuth{
		DeviceCode:              deref(d.DeviceCode),
		UserCode:                deref(d.UserCode),
		VerificationURI:         deref(d.VerificationUri),
		VerificationURIComplete: deref(d.VerificationUriComplete),
		ExpiresIn:               derefInt32(d.ExpiresIn),
		Interval:                derefInt32(d.Interval),
	}, nil
}

// PollDeviceLogin checks whether the user has authorized the OIDC device flow.
func (s *AuthService) PollDeviceLogin(ctx context.Context, deviceCode string) (DevicePoll, error) {
	resp, err := s.api.PollOidcDeviceLoginWithResponse(ctx, client.PollOidcDeviceLoginJSONRequestBody{
		DeviceCode: deviceCode,
	})
	if err != nil {
		return DevicePoll{}, fmt.Errorf("poll device login: %w", err)
	}
	if resp.JSON200 == nil {
		return DevicePoll{}, &APIError{Status: resp.StatusCode()}
	}

	p := resp.JSON200
	poll := DevicePoll{Status: DeviceStatus(strings.ToUpper(deref(p.Status)))}
	if poll.Status == DeviceComplete {
		tokens, err := toTokenPair(p.AccessToken, p.RefreshToken)
		if err != nil {
			return DevicePoll{}, err
		}
		poll.Tokens = tokens
	}
	return poll, nil
}

// Logout tells the server to revoke the session. Local tokens are cleared.
func (s *AuthService) Logout(ctx context.Context) error {
	resp, err := s.api.LogoutWithResponse(ctx)
	if err != nil {
		return fmt.Errorf("logout: %w", err)
	}
	if resp.StatusCode() != http.StatusOK {
		return &APIError{Status: resp.StatusCode()}
	}
	return nil
}

func toSystemInfo(d *client.SystemInfoDetails) SystemInfo {
	info := SystemInfo{
		ServerName: deref(d.ServerName),
		Version:    deref(d.Version),
	}
	if d.Setup != nil {
		info.Setup = toSetup(d.Setup)
	}
	return info
}

func toSetup(s *client.Setup) Setup {
	setup := Setup{
		AuthMode:      deref(s.AuthMode),
		EmailRequired: derefBool(s.EmailRequired),
		AllowSignup:   derefBool(s.AllowSignup),
	}
	if s.Oidc != nil {
		setup.OIDC = &OIDC{
			ProviderName: deref(s.Oidc.ProviderName),
			IssuerURI:    deref(s.Oidc.IssuerUri),
			ClientID:     deref(s.Oidc.ClientId),
		}
	}
	return setup
}

func toTokenPair(access, refresh *string) (TokenPair, error) {
	if access == nil || refresh == nil {
		return TokenPair{}, ErrIncompleteResponse
	}
	return TokenPair{Access: *access, Refresh: *refresh}, nil
}

func deref(p *string) string {
	if p == nil {
		return ""
	}
	return *p
}

func derefBool(p *bool) bool {
	return p != nil && *p
}

func derefInt32(p *int32) int {
	if p == nil {
		return 0
	}
	return int(*p)
}
