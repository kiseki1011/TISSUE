package domain

import (
	"context"
	"fmt"
	"sort"
	"time"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// IssueActivity is one entry in an issue's audit trail: who did what, when, and which fields moved.
type IssueActivity struct {
	ID         int64
	Type       string // raw event enum, e.g. ISSUE_WORKFLOW_TRANSITIONED
	ActorID    int64  // 0 for a system actor
	OccurredAt time.Time
	Changes    []ActivityChange
	Data       []ActivityData // extra event metadata (key/value), sorted by key
}

// ActivityChange is one field's before/after value for an activity entry.
type ActivityChange struct {
	Field string
	From  string
	To    string
}

// ActivityData is one extra metadata pair carried by an activity event.
type ActivityData struct {
	Key   string
	Value string
}

// IssueActivityPage is a page of activity entries. The log is cursor-paginated (no total), so HasNext
// is all that is needed to hint that more history exists.
type IssueActivityPage struct {
	Items   []IssueActivity
	HasNext bool
}

// ListIssueActivities fetches the most recent page of an issue's activity log.
func (s *IssueService) ListIssueActivities(ctx context.Context, issueKey string, limit int) (IssueActivityPage, error) {
	l := int32(limit)
	resp, err := s.api.ListIssueActivitiesWithResponse(ctx, issueKey, &client.ListIssueActivitiesParams{Limit: &l})
	if err != nil {
		return IssueActivityPage{}, fmt.Errorf("list issue activities: %w", err)
	}
	if resp.JSON200 == nil {
		return IssueActivityPage{}, newAPIError(resp.StatusCode(), resp.Body)
	}
	return toActivityPage(resp.JSON200), nil
}

func toActivityPage(p *client.CursorPageActivityLogResponse) IssueActivityPage {
	out := IssueActivityPage{HasNext: derefBool(p.HasNext)}
	if p.Content != nil {
		for _, a := range *p.Content {
			out.Items = append(out.Items, toActivity(a))
		}
	}
	return out
}

func toActivity(a client.ActivityLogResponse) IssueActivity {
	out := IssueActivity{
		ID:         derefInt64to64(a.Id),
		Type:       enumStr(a.Type),
		ActorID:    derefInt64to64(a.ActorMemberId),
		OccurredAt: derefTime(a.OccurredAt),
	}
	if a.Changes != nil {
		for field, ch := range *a.Changes {
			out.Changes = append(out.Changes, ActivityChange{Field: field, From: changeVal(ch.From), To: changeVal(ch.To)})
		}
		// the changes come from a map, so sort by field for a deterministic order
		sort.Slice(out.Changes, func(i, j int) bool { return out.Changes[i].Field < out.Changes[j].Field })
	}
	if a.Data != nil {
		for k, v := range *a.Data {
			out.Data = append(out.Data, ActivityData{Key: k, Value: v})
		}
		sort.Slice(out.Data, func(i, j int) bool { return out.Data[i].Key < out.Data[j].Key })
	}
	return out
}

// changeVal renders an activity change value (an untyped JSON scalar) as a string, mapping a missing
// value to empty.
func changeVal(v interface{}) string {
	if v == nil {
		return ""
	}
	return fmt.Sprintf("%v", v)
}
