package domain

import (
	"os"
	"path/filepath"
	"strings"
	"time"
)

// localZoneCache memoises the resolved zone id. Resolution can touch the filesystem, and the value is
// fixed for the life of the process (Go reads the zone once at startup), so repeat lookups are wasted.
var localZoneCache string

// LocalZoneID reports the timezone to bucket a server-side day series on, in the form the backend's
// ZoneId.of understands. Bucketing instants into days is a calendar operation, so the server has to be
// told which calendar to use; left unsaid it cuts on UTC, which puts a Seoul morning on the previous day.
//
// An IANA id ("Asia/Seoul") is preferred over a fixed offset because a stats window can span a daylight
// saving transition, which a single offset gets wrong on one side of it. Go does not hand the id over
// directly - time.Local.String() reports "Local" whenever the zone came from /etc/localtime rather than
// $TZ - so this walks the usual sources in order of trustworthiness and only then settles for the
// current offset, which is still far better than defaulting to UTC.
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
	return time.Now().Format("-07:00") // e.g. "+09:00": right today, and parseable by the server
}

// isLoadableZone reports whether name is an IANA id this machine can actually resolve, so a stale or
// misspelled $TZ is not forwarded to the server (which would silently fall back to UTC).
func isLoadableZone(name string) bool {
	if name == "" || name == "Local" {
		return false
	}
	_, err := time.LoadLocation(name)
	return err == nil
}

// zoneIDFromLocaltimeLink recovers the IANA id from /etc/localtime, which on macOS and Linux is a
// symlink into the zoneinfo tree ("…/zoneinfo/Asia/Seoul"). This is the only place the name survives
// when $TZ is unset, which is the common case.
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
