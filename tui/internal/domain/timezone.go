package domain

import (
	"os"
	"path/filepath"
	"strings"
	"time"
)

// localZoneCache memoises the resolved zone id. Resolution touches the filesystem and cannot change.
var localZoneCache string

// LocalZoneID reports the timezone to bucket a server-side day series on. Left unsaid, the server cuts
// days on UTC, putting a Seoul morning on the previous day. An IANA id beats a fixed offset because a
// window can span a DST transition, but Go reports only "Local" when the zone came from /etc/localtime.
func LocalZoneID() string {
	if localZoneCache == "" {
		localZoneCache = resolveLocalZoneID()
	}
	return localZoneCache
}

func resolveLocalZoneID() string {
	if tz := os.Getenv("TZ"); isLoadableZone(tz) {
		return tz
	}
	if name := time.Local.String(); name != "Local" && isLoadableZone(name) {
		return name
	}
	if id, ok := zoneIDFromLocaltimeLink(); ok {
		return id
	}
	return time.Now().Format("-07:00") // e.g. "+09:00", parseable by the server
}

// isLoadableZone guards against forwarding a stale $TZ, which would silently drop the server to UTC.
func isLoadableZone(name string) bool {
	if name == "" || name == "Local" {
		return false
	}
	_, err := time.LoadLocation(name)
	return err == nil
}

// zoneIDFromLocaltimeLink reads the id out of the /etc/localtime symlink, the only place the name
// survives when $TZ is unset (the common case).
func zoneIDFromLocaltimeLink() (string, bool) {
	target, err := filepath.EvalSymlinks("/etc/localtime")
	if err != nil {
		return "", false
	}
	target = filepath.ToSlash(target)
	i := strings.LastIndex(target, "/zoneinfo/")
	if i < 0 {
		return "", false
	}
	id := strings.TrimPrefix(target[i+len("/zoneinfo/"):], "/")
	if !isLoadableZone(id) {
		return "", false
	}
	return id, true
}
