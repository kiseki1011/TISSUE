// Wraps the generated API client and turns its DTOs into values the UI can use directly.
package domain

import "strings"

// TokenPair is an access/refresh token pair issued by the server.
type TokenPair struct {
	Access  string
	Refresh string
}

// SystemInfo is the server's configuration, read before login to pick the authentication flow.
type SystemInfo struct {
	ServerName string
	Version    string
	Setup      Setup
}

// Setup is the server's authentication configuration.
type Setup struct {
	AuthMode      string
	EmailRequired bool
	AllowSignup   bool
	OIDC          *OIDC
}

// IsOIDC reports whether the server authenticates through an external IdP.
func (s Setup) IsOIDC() bool {
	return strings.EqualFold(s.AuthMode, "OIDC")
}

// OIDC describes the external IdP, present only in OIDC mode.
type OIDC struct {
	ProviderName string
	IssuerURI    string
	ClientID     string
}

// DeviceAuth starts the OIDC device flow. The user enters `UserCode` at `VerificationURI` while the
// client polls until they authorize.
type DeviceAuth struct {
	DeviceCode              string
	UserCode                string
	VerificationURI         string
	VerificationURIComplete string
	ExpiresIn               int
	Interval                int
}

// DeviceStatus is the state of an in-progress OIDC device login.
type DeviceStatus string

const (
	DevicePending  DeviceStatus = "PENDING"
	DeviceComplete DeviceStatus = "COMPLETE"
	DeviceSlowDown DeviceStatus = "SLOW_DOWN"
	DeviceDenied   DeviceStatus = "DENIED"
	DeviceExpired  DeviceStatus = "EXPIRED"
	DeviceError    DeviceStatus = "ERROR"
)

// DevicePoll is one device-flow poll result. Tokens are set only when Status is DeviceComplete.
type DevicePoll struct {
	Status DeviceStatus
	Tokens TokenPair
}
