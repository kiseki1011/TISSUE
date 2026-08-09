package domain

import (
	"context"
	"fmt"
	"strings"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// RelationType mirrors the backend enum. Direction is source -> target: RELEVANT is a bidirectional
// informational link; BLOCKS/CAUSES/DUPLICATES are directional (source blocks/causes/duplicates target).
type RelationType string

const (
	RelationRelevant   RelationType = "RELEVANT"
	RelationBlocks     RelationType = "BLOCKS"
	RelationCauses     RelationType = "CAUSES"
	RelationDuplicates RelationType = "DUPLICATES"
)

// AddRelation links issueKey (the source) to targetKey via rel. targetProjectKey is required by the
// request DTO though the backend resolves the target by its globally-unique key alone. The server rejects
// cycles (for the directional types), duplicates and self-links, surfaced here as the APIError status.
func (s *IssueService) AddRelation(ctx context.Context, issueKey, targetProjectKey, targetKey string, rel RelationType) error {
	resp, err := s.api.AddIssueRelationWithResponse(ctx, issueKey, client.AddIssueRelationRequest{
		RelationType:     client.AddIssueRelationRequestRelationType(rel),
		TargetIssueKey:   targetKey,
		TargetProjectKey: targetProjectKey,
	})
	if err != nil {
		return fmt.Errorf("add relation: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// RelatedIssue is one issue linked to the viewed issue through a relation.
type RelatedIssue struct {
	Key           string
	Title         string
	TypeName      string
	StateLabel    string
	StateCategory string // INITIAL | ACTIVE | COMPLETED | ABORTED
	Priority      string // P0 .. P4
}

// IssueRelationGroup is the set of related issues sharing one relation kind (Blocks, Blocked by, ...).
type IssueRelationGroup struct {
	Kind  string // display label
	Items []RelatedIssue
	// Removable reports whether this kind can be unlinked from the viewed issue. A directional relation
	// (BLOCKS/CAUSES/DUPLICATES) lives on its source, so its inverse group ("Blocked by", ...) has to be
	// removed from the other issue - asking here returns RELATION_NOT_FOUND. RELEVANT is symmetric.
	Removable bool
}

// toRelationGroups flattens the relations detail into display groups, in a fixed order, omitting the
// kinds with no linked issues (the common case) so the section stays compact.
func toRelationGroups(r *client.IssueRelationsDetail) []IssueRelationGroup {
	defs := []struct {
		label     string
		list      *[]client.RelatedIssueInfo
		removable bool // false for the inverse of a directional relation, which only its source can remove
	}{
		{"Blocks", r.Blocks, true},
		{"Blocked by", r.BlockedBy, false},
		{"Causes", r.Causes, true},
		{"Caused by", r.CausedBy, false},
		{"Duplicates", r.Duplicates, true},
		{"Duplicated by", r.DuplicatedBy, false},
		{"Related to", r.Relevant, true},
	}
	var groups []IssueRelationGroup
	for _, d := range defs {
		if d.list == nil || len(*d.list) == 0 {
			continue
		}
		g := IssueRelationGroup{Kind: d.label, Removable: d.removable}
		for _, ri := range *d.list {
			g.Items = append(g.Items, toRelatedIssue(ri))
		}
		groups = append(groups, g)
	}
	return groups
}

func toRelatedIssue(ri client.RelatedIssueInfo) RelatedIssue {
	out := RelatedIssue{
		Key:      deref(ri.IssueKey),
		Title:    deref(ri.Title),
		Priority: enumStr(ri.Priority),
	}
	if ri.IssueType != nil {
		out.TypeName = deref(ri.IssueType.DisplayName)
	}
	if ri.CurrentState != nil {
		out.StateLabel = deref(ri.CurrentState.DisplayName)
		out.StateCategory = enumStr(ri.CurrentState.Category)
	}
	return out
}

// RemoveRelation unlinks targetKey from issueKey. Only a relation the viewed issue owns can be removed
// from it: a directional relation must be removed from its source, and asking from the target returns
// RELATION_NOT_FOUND. targetProjectKey is required by the request DTO though the server resolves the
// target by its globally-unique key alone.
func (s *IssueService) RemoveRelation(ctx context.Context, issueKey, targetProjectKey, targetKey string) error {
	resp, err := s.api.RemoveIssueRelationWithResponse(ctx, issueKey, client.RemoveIssueRelationRequest{
		TargetIssueKey:   targetKey,
		TargetProjectKey: targetProjectKey,
	})
	if err != nil {
		return fmt.Errorf("remove relation: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// ProjectKeyOf is the project an issue key belongs to: everything before its trailing "-<number>". It
// mirrors how the server derives the project from a globally-unique issue key, so a cross-project link
// can be addressed without looking the issue up.
func ProjectKeyOf(issueKey string) string {
	if i := strings.LastIndex(issueKey, "-"); i > 0 {
		return issueKey[:i]
	}
	return issueKey
}
