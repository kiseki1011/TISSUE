package domain

import (
	"context"
	"fmt"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// NotificationService is the caller's inbox. Notifications are member-scoped, not per project.
type NotificationService struct {
	api *client.ClientWithResponses
}

func NewNotificationService(api *client.ClientWithResponses) *NotificationService {
	return &NotificationService{api: api}
}

// List returns one cursor page. An empty cursor fetches the newest page.
// unreadOnly and mentionsOnly filter server-side, so infinite scroll stays correct.
func (s *NotificationService) List(ctx context.Context, unreadOnly, mentionsOnly bool, cursor string, limit int) (NotificationPage, error) {
	params := &client.ListNotificationsParams{}
	if unreadOnly {
		params.UnreadOnly = &unreadOnly
	}
	if mentionsOnly {
		params.Types = &[]client.ListNotificationsParamsTypes{client.ListNotificationsParamsTypesISSUEMENTIONED}
	}
	if cursor != "" {
		params.Cursor = &cursor
	}
	if limit > 0 {
		l := int32Of(limit)
		params.Limit = &l
	}
	resp, err := s.api.ListNotificationsWithResponse(ctx, params)
	if err != nil {
		return NotificationPage{}, fmt.Errorf("list notifications: %w", err)
	}
	if resp.JSON200 == nil {
		return NotificationPage{}, newAPIError(resp.StatusCode(), resp.Body)
	}
	return toNotificationPage(resp.JSON200), nil
}

// HasUnread drives the Inbox badge. The endpoint returns a boolean, no count, so the badge is a dot.
func (s *NotificationService) HasUnread(ctx context.Context) (bool, error) {
	resp, err := s.api.CheckNotificationUnreadStatusWithResponse(ctx)
	if err != nil {
		return false, fmt.Errorf("check unread status: %w", err)
	}
	if resp.JSON200 == nil {
		return false, newAPIError(resp.StatusCode(), resp.Body)
	}
	return *resp.JSON200, nil
}

// MarkRead marks a single notification read (idempotent server-side).
func (s *NotificationService) MarkRead(ctx context.Context, id int64) error {
	resp, err := s.api.ReadNotificationWithResponse(ctx, id)
	if err != nil {
		return fmt.Errorf("read notification: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

func (s *NotificationService) MarkAllRead(ctx context.Context) error {
	resp, err := s.api.ReadAllNotificationsWithResponse(ctx)
	if err != nil {
		return fmt.Errorf("read all notifications: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// GetPreferences returns the caller's delivery preferences, one row per type × channel.
func (s *NotificationService) GetPreferences(ctx context.Context) ([]NotificationPref, error) {
	resp, err := s.api.GetNotificationPreferencesWithResponse(ctx)
	if err != nil {
		return nil, fmt.Errorf("get notification preferences: %w", err)
	}
	if resp.JSON200 == nil {
		return nil, newAPIError(resp.StatusCode(), resp.Body)
	}
	out := make([]NotificationPref, 0, len(*resp.JSON200))
	for _, p := range *resp.JSON200 {
		out = append(out, NotificationPref{Type: enumStr(p.Type), Channel: enumStr(p.Channel), Enabled: derefBool(p.Enabled)})
	}
	return out, nil
}

func (s *NotificationService) UpdatePreference(ctx context.Context, notifType, channel string, enabled bool) error {
	body := client.UpdateNotificationPreferenceRequest{
		Type:    client.UpdateNotificationPreferenceRequestType(notifType),
		Channel: client.UpdateNotificationPreferenceRequestChannel(channel),
		Enabled: &enabled,
	}
	resp, err := s.api.UpdateNotificationPreferencesWithResponse(ctx, body)
	if err != nil {
		return fmt.Errorf("update notification preference: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

func toNotificationPage(p *client.CursorPageNotificationResponse) NotificationPage {
	out := NotificationPage{HasNext: derefBool(p.HasNext), NextCursor: deref(p.NextCursor)}
	if p.Content != nil {
		out.Items = make([]Notification, 0, len(*p.Content))
		for _, n := range *p.Content {
			out.Items = append(out.Items, toNotification(n))
		}
	}
	return out
}

func toNotification(n client.NotificationResponse) Notification {
	out := Notification{
		ID:        derefInt64to64(n.Id),
		Type:      enumStr(n.Type),
		ActorName: deref(n.ActorDisplayName),
		IsRead:    derefBool(n.IsRead),
		CreatedAt: derefTime(n.CreatedAt),
	}
	if n.Data != nil {
		out.Data = *n.Data
	}
	if n.EntityReference != nil {
		out.Ref = EntityRef{
			ResourceType: enumStr(n.EntityReference.ResourceType),
			ResourceID:   derefInt64to64(n.EntityReference.ResourceId),
			ProjectKey:   deref(n.EntityReference.ProjectKey),
			IssueKey:     deref(n.EntityReference.IssueKey),
			MemberID:     derefInt64to64(n.EntityReference.MemberId),
		}
	}
	return out
}
