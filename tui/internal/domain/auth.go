// Package domain wraps the generated API client and turns its DTOs into UI-ready values.
package domain

import "strings"

type TokenPair struct {
	Access  string
	Refresh string
}

// SystemInfo is read before login to pick the authentication flow.
type SystemInfo struct {
	ServerName string
	Version    string
	Setup      Setup
}

type Setup struct {
	AuthMode      string
	EmailRequired bool
	AllowSignup   bool
	OIDC          *OIDC
}

func (s Setup) IsOIDC() bool {
	return strings.EqualFold(s.AuthMode, "OIDC")
}

// OIDC is present only in OIDC mode.
type OIDC struct {
	ProviderName string
	IssuerURI    string
	ClientID     string
}

// DeviceAuth starts the OIDC device flow. The user enters UserCode at VerificationURI while the client polls.
type DeviceAuth struct {
	DeviceCode              string
	UserCode                string
	VerificationURI         string
	VerificationURIComplete string
	ExpiresIn               int
	Interval                int
}

type DeviceStatus string

const (
	DevicePending  DeviceStatus = "PENDING"
	DeviceComplete DeviceStatus = "COMPLETE"
	DeviceSlowDown DeviceStatus = "SLOW_DOWN"
	DeviceDenied   DeviceStatus = "DENIED"
	DeviceExpired  DeviceStatus = "EXPIRED"
	DeviceError    DeviceStatus = "ERROR"
)

// DevicePoll's Tokens are set only when Status is DeviceComplete.
type DevicePoll struct {
	Status DeviceStatus
	Tokens TokenPair
}
