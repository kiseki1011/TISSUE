package domain

import (
	"context"
	"fmt"
	"sort"

	"github.com/oapi-codegen/nullable"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// CatalogService reads and edits the instance-wide catalogs.
// Reads are open to any authenticated member. Edits are admin-only.
type CatalogService struct {
	api *client.ClientWithResponses
}

func NewCatalogService(api *client.ClientWithResponses) *CatalogService {
	return &CatalogService{api: api}
}

type IssueTypeSummary struct {
	ID             int
	Name           string
	Description    string
	Hierarchy      string // EPIC, STANDARD, SUBTASK, MICROTASK
	WorkflowName   string
	Color          string // ColorType enum name, such as ANSI_RED, INDIGO
	SystemProvided bool
}

type WorkflowSummary struct {
	ID             int
	Name           string
	Description    string
	SystemProvided bool
}

type IssueField struct {
	ID          int
	Name        string
	Type        string // TEXT, SHORT_TEXT, SELECT_OPTION, CHECKLIST, BOOLEAN, DATE, ...
	Required    bool
	Description string
	Position    int           // display order among the type's fields (append = max+1)
	Options     []FieldOption // for SELECT_OPTION / CHECKLIST
}

// FieldOption is one option on a SELECT_OPTION / CHECKLIST field. Its id drives the per-option endpoints.
type FieldOption struct {
	ID   int
	Name string
}

type IssueTypeDetail struct {
	ID             int
	Name           string
	Description    string
	Hierarchy      string
	WorkflowName   string
	Color          string
	SystemProvided bool
	Fields         []IssueField
}

func (s *CatalogService) GetIssueType(ctx context.Context, id int) (IssueTypeDetail, error) {
	resp, err := s.api.GetIssueTypeWithResponse(ctx, int64(id))
	if err != nil {
		return IssueTypeDetail{}, fmt.Errorf("get issue type: %w", err)
	}
	if resp.JSON200 == nil {
		return IssueTypeDetail{}, newAPIError(resp.StatusCode(), resp.Body)
	}
	d := resp.JSON200
	hierarchy := ""
	if d.Hierarchy != nil {
		hierarchy = string(*d.Hierarchy)
	}
	color := ""
	if d.Color != nil {
		color = string(*d.Color)
	}
	var raw []client.IssueFieldDetail
	if d.Fields != nil {
		raw = append(raw, *d.Fields...)
	}
	sort.SliceStable(raw, func(i, j int) bool {
		return derefInt32(raw[i].Position) < derefInt32(raw[j].Position)
	})
	fields := make([]IssueField, 0, len(raw))
	for _, f := range raw {
		ftype := ""
		if f.Type != nil {
			ftype = string(*f.Type)
		}
		var opts []FieldOption
		if f.Options != nil {
			for _, o := range *f.Options {
				opts = append(opts, FieldOption{ID: derefInt64(o.Id), Name: deref(o.Name)})
			}
		}
		fields = append(fields, IssueField{
			ID:          derefInt64(f.Id),
			Name:        deref(f.Name),
			Type:        ftype,
			Required:    derefBool(f.Required),
			Description: deref(f.Description),
			Position:    int(derefInt32(f.Position)),
			Options:     opts,
		})
	}
	return IssueTypeDetail{
		ID:             derefInt64(d.Id),
		Name:           deref(d.Name),
		Description:    deref(d.Description),
		Hierarchy:      hierarchy,
		WorkflowName:   deref(d.WorkflowName),
		Color:          color,
		SystemProvided: derefBool(d.SystemProvided),
		Fields:         fields,
	}, nil
}

type WorkflowState struct {
	ID          int
	Label       string
	Category    string // INITIAL, ACTIVE, COMPLETED, ABORTED
	Color       string // ColorType enum name
	Description string
}

type WorkflowGuard struct {
	Type   string // GuardType enum name
	Order  int
	Params map[string]any
}

type WorkflowTransition struct {
	ID          int
	Label       string
	Description string
	SourceID    int
	TargetID    int
	Guards      []WorkflowGuard
}

type WorkflowDetail struct {
	ID             int
	Name           string
	Description    string
	Color          string
	SystemProvided bool
	InitialStateID int
	// Version is the optimistic-lock value a whole-graph replace must echo back.
	Version int
	// Transitions auto-fired when a linked PR is opened/merged. 0 means unset.
	VcsPrOpenedTransitionID int
	VcsPrMergedTransitionID int
	States                  []WorkflowState
	Transitions             []WorkflowTransition
}

func (s *CatalogService) GetWorkflow(ctx context.Context, id int) (WorkflowDetail, error) {
	resp, err := s.api.GetWorkflowWithResponse(ctx, int64(id))
	if err != nil {
		return WorkflowDetail{}, fmt.Errorf("get workflow: %w", err)
	}
	if resp.JSON200 == nil {
		return WorkflowDetail{}, newAPIError(resp.StatusCode(), resp.Body)
	}
	d := resp.JSON200
	out := WorkflowDetail{
		ID:                      derefInt64(d.Id),
		Name:                    deref(d.Name),
		Description:             deref(d.Description),
		SystemProvided:          derefBool(d.IsSystemProvided),
		InitialStateID:          derefInt64(d.InitialStateId),
		Version:                 derefInt64(d.Version),
		VcsPrOpenedTransitionID: derefInt64(d.VcsPrOpenedTransitionId),
		VcsPrMergedTransitionID: derefInt64(d.VcsPrMergedTransitionId),
	}
	if d.Color != nil {
		out.Color = string(*d.Color)
	}
	if d.States != nil {
		for _, st := range *d.States {
			ws := WorkflowState{
				ID:          derefInt64(st.Id),
				Label:       deref(st.Label),
				Description: deref(st.Description),
			}
			if st.Category != nil {
				ws.Category = string(*st.Category)
			}
			if st.Color != nil {
				ws.Color = string(*st.Color)
			}
			out.States = append(out.States, ws)
		}
	}
	if d.Transitions != nil {
		for _, tr := range *d.Transitions {
			wt := WorkflowTransition{
				ID:          derefInt64(tr.Id),
				Label:       deref(tr.Label),
				Description: deref(tr.Description),
				SourceID:    derefInt64(tr.SourceStateId),
				TargetID:    derefInt64(tr.TargetStateId),
			}
			if tr.Guards != nil {
				for _, gd := range *tr.Guards {
					wg := WorkflowGuard{Order: int(derefInt32(gd.Order))}
					if gd.GuardType != nil {
						wg.Type = string(*gd.GuardType)
					}
					if gd.Params != nil {
						wg.Params = *gd.Params
					}
					wt.Guards = append(wt.Guards, wg)
				}
				sort.SliceStable(wt.Guards, func(i, j int) bool { return wt.Guards[i].Order < wt.Guards[j].Order })
			}
			out.Transitions = append(out.Transitions, wt)
		}
	}
	return out, nil
}

// UpdateWorkflowState cannot change a state's category or wiring. Those go through the whole-graph replace.
func (s *CatalogService) UpdateWorkflowState(ctx context.Context, workflowID, stateID int, name, color, description string) error {
	body := client.UpdateWorkflowStateJSONRequestBody{
		Name:        nullable.NewNullableWithValue(name),
		Color:       nullable.NewNullableWithValue(color),
		Description: nullable.NewNullableWithValue(description),
	}
	resp, err := s.api.UpdateWorkflowStateWithResponse(ctx, int64(workflowID), int64(stateID), body)
	if err != nil {
		return fmt.Errorf("update workflow state: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// UpdateWorkflowVcsSettings replaces the whole PR-opened/merged transition mapping. A zero id clears it.
func (s *CatalogService) UpdateWorkflowVcsSettings(ctx context.Context, workflowID, openedTransitionID, mergedTransitionID int) error {
	var body client.UpdateWorkflowVcsSettingsJSONRequestBody
	if openedTransitionID != 0 {
		v := int64(openedTransitionID)
		body.VcsPrOpenedTransitionId = &v
	}
	if mergedTransitionID != 0 {
		v := int64(mergedTransitionID)
		body.VcsPrMergedTransitionId = &v
	}
	resp, err := s.api.UpdateWorkflowVcsSettingsWithResponse(ctx, int64(workflowID), body)
	if err != nil {
		return fmt.Errorf("update workflow vcs settings: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// UpdateWorkflowTransition cannot rewire a transition. That goes through the whole-graph replace.
func (s *CatalogService) UpdateWorkflowTransition(ctx context.Context, workflowID, transitionID int, name, description string) error {
	body := client.UpdateWorkflowTransitionJSONRequestBody{
		Name:        nullable.NewNullableWithValue(name),
		Description: nullable.NewNullableWithValue(description),
	}
	resp, err := s.api.UpdateWorkflowTransitionWithResponse(ctx, int64(workflowID), int64(transitionID), body)
	if err != nil {
		return fmt.Errorf("update workflow transition: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

type GuardInput struct {
	Type   string
	Order  int
	Params map[string]any
}

// ConfigureTransitionGuards replaces the whole guard list. The backend rejects an empty list.
func (s *CatalogService) ConfigureTransitionGuards(ctx context.Context, workflowID, transitionID int, guards []GuardInput) error {
	body := client.ConfigureTransitionGuardsJSONRequestBody{Guards: make([]client.GuardConfigData, len(guards))}
	for i, g := range guards {
		gc := client.GuardConfigData{
			GuardType: client.GuardConfigDataGuardType(g.Type),
			Order:     int32(g.Order), //nolint:gosec // order is a small positive index
		}
		if len(g.Params) > 0 {
			p := g.Params
			gc.Params = &p
		}
		body.Guards[i] = gc
	}
	resp, err := s.api.ConfigureTransitionGuardsWithResponse(ctx, int64(workflowID), int64(transitionID), body)
	if err != nil {
		return fmt.Errorf("configure transition guards: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// GraphRef points at an existing state by ID or a new one by TempKey. Exactly one is set.
type GraphRef struct {
	ID      int
	TempKey string
}

// GraphStateInput is one state in a whole-graph replace. Existing states carry ID and can change
// only Category. New states carry TempKey plus Name and Color.
type GraphStateInput struct {
	ID          int
	TempKey     string
	Name        string
	Description string
	Color       string
	Category    string
}

// GraphTransitionInput is one transition in a whole-graph replace. Existing ones carry ID and may
// be rewired. New ones carry TempKey plus Name.
type GraphTransitionInput struct {
	ID          int
	TempKey     string
	Name        string
	Description string
	Source      GraphRef
	Target      GraphRef
}

// ReplaceWorkflowGraph swaps a workflow's whole topology. Omitted states and transitions are deleted.
// version is the optimistic lock. Deleting a state that still holds active issues needs a migration.
func (s *CatalogService) ReplaceWorkflowGraph(
	ctx context.Context, workflowID, version int, states []GraphStateInput, transitions []GraphTransitionInput,
) error {
	body := client.ReplaceWorkflowGraphJSONRequestBody{Version: int64(version)}
	for _, st := range states {
		r := client.ReplaceStatusRequest{Category: client.ReplaceStatusRequestCategory(st.Category)}
		if st.ID != 0 {
			id := int64(st.ID)
			r.Id = &id
		} else if st.TempKey != "" {
			tk := st.TempKey
			r.TempKey = &tk
		}
		if st.Name != "" {
			n := st.Name
			r.Name = &n
		}
		if st.Description != "" {
			ds := st.Description
			r.Description = &ds
		}
		if st.Color != "" {
			c := client.ReplaceStatusRequestColor(st.Color)
			r.Color = &c
		}
		body.ReplaceStatusRequests = append(body.ReplaceStatusRequests, r)
	}
	for _, tr := range transitions {
		r := client.ReplaceTransitionRequest{Source: graphRef(tr.Source), Target: graphRef(tr.Target)}
		if tr.ID != 0 {
			id := int64(tr.ID)
			r.Id = &id
		} else if tr.TempKey != "" {
			tk := tr.TempKey
			r.TempKey = &tk
		}
		if tr.Name != "" {
			n := tr.Name
			r.Name = &n
		}
		if tr.Description != "" {
			ds := tr.Description
			r.Description = &ds
		}
		body.ReplaceTransitionRequests = append(body.ReplaceTransitionRequests, r)
	}
	resp, err := s.api.ReplaceWorkflowGraphWithResponse(ctx, int64(workflowID), body)
	if err != nil {
		return fmt.Errorf("replace workflow graph: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

func graphRef(r GraphRef) client.Ref {
	if r.ID != 0 {
		id := int64(r.ID)
		return client.Ref{Id: &id}
	}
	tk := r.TempKey
	return client.Ref{TempKey: &tk}
}

// UpdateIssueField cannot change a field's type, which is fixed at creation. Options have their own endpoints.
func (s *CatalogService) UpdateIssueField(ctx context.Context, fieldID int, name, description string, required bool) error {
	body := client.UpdateIssueFieldJSONRequestBody{
		Name:        nullable.NewNullableWithValue(name),
		Description: nullable.NewNullableWithValue(description),
		Required:    nullable.NewNullableWithValue(required),
	}
	resp, err := s.api.UpdateIssueFieldWithResponse(ctx, int64(fieldID), body)
	if err != nil {
		return fmt.Errorf("update issue field: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// CreateIssueField adds a custom field. The backend does not auto-append, so the caller passes
// position as max(existing)+1. initialOptions apply only to SELECT_OPTION / CHECKLIST.
func (s *CatalogService) CreateIssueField(
	ctx context.Context, typeID int, name, description, fieldType string, required bool, position int, initialOptions []string,
) error {
	body := client.CreateIssueFieldJSONRequestBody{
		Name:     name,
		Type:     client.CreateIssueFieldRequestType(fieldType),
		Required: &required,
		Position: int32(position), //nolint:gosec // position is a small non-negative index
	}
	if description != "" {
		body.Description = &description
	}
	if len(initialOptions) > 0 {
		opts := initialOptions
		body.InitialOptions = &opts
	}
	resp, err := s.api.CreateIssueFieldWithResponse(ctx, int64(typeID), body)
	if err != nil {
		return fmt.Errorf("create issue field: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// DeleteIssueField is rejected with 409 if any issue still holds a value for the field.
func (s *CatalogService) DeleteIssueField(ctx context.Context, fieldID int) error {
	resp, err := s.api.DeleteIssueFieldWithResponse(ctx, int64(fieldID))
	if err != nil {
		return fmt.Errorf("delete issue field: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// ReorderIssueFields sets field order from the given id sequence (position = list index).
// Pass the complete id list. Omitted ids keep their old position and unknown ids are ignored.
func (s *CatalogService) ReorderIssueFields(ctx context.Context, typeID int, orderedIDs []int) error {
	ids := make([]int64, len(orderedIDs))
	for i, id := range orderedIDs {
		ids[i] = int64(id)
	}
	body := client.ReorderIssueTypeFieldsJSONRequestBody{OrderedIds: ids}
	resp, err := s.api.ReorderIssueTypeFieldsWithResponse(ctx, int64(typeID), body)
	if err != nil {
		return fmt.Errorf("reorder issue fields: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

func (s *CatalogService) AddFieldOption(ctx context.Context, fieldID int, optionName string) error {
	body := client.AddIssueFieldOptionJSONRequestBody{OptionName: optionName}
	resp, err := s.api.AddIssueFieldOptionWithResponse(ctx, int64(fieldID), body)
	if err != nil {
		return fmt.Errorf("add field option: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

func (s *CatalogService) RenameFieldOption(ctx context.Context, fieldID, optionID int, name string) error {
	body := client.UpdateIssueFieldOptionJSONRequestBody{Name: name}
	resp, err := s.api.UpdateIssueFieldOptionWithResponse(ctx, int64(fieldID), int64(optionID), body)
	if err != nil {
		return fmt.Errorf("rename field option: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// DeleteFieldOption is rejected with 409 if an issue still uses the option.
func (s *CatalogService) DeleteFieldOption(ctx context.Context, fieldID, optionID int) error {
	resp, err := s.api.DeleteIssueFieldOptionWithResponse(ctx, int64(fieldID), int64(optionID))
	if err != nil {
		return fmt.Errorf("delete field option: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// CreateIssueType creates a global issue type. hierarchy is EPIC, STANDARD, SUBTASK, or MICROTASK.
// The icon is left out of the request. The TUI never surfaces it and the server defaults it.
func (s *CatalogService) CreateIssueType(ctx context.Context, name, description, color, hierarchy string, workflowID int) (int, error) {
	body := client.CreateIssueTypeJSONRequestBody{
		Name:           name,
		Color:          client.CreateIssueTypeRequestColor(color),
		IssueHierarchy: client.CreateIssueTypeRequestIssueHierarchy(hierarchy),
		WorkflowId:     int64(workflowID),
	}
	if description != "" {
		body.Description = &description
	}
	resp, err := s.api.CreateIssueTypeWithResponse(ctx, body)
	if err != nil {
		return 0, fmt.Errorf("create issue type: %w", err)
	}
	if resp.StatusCode() < 200 || resp.StatusCode() >= 300 {
		return 0, newAPIError(resp.StatusCode(), resp.Body)
	}
	if resp.JSON201 != nil {
		return derefInt64(resp.JSON201.IssueTypeId), nil
	}
	return 0, nil
}

// UpdateIssueType cannot change hierarchy or workflow. Both are fixed at creation.
func (s *CatalogService) UpdateIssueType(ctx context.Context, id int, name, color, description string) error {
	body := client.UpdateIssueTypeJSONRequestBody{
		Name:        nullable.NewNullableWithValue(name),
		Color:       nullable.NewNullableWithValue(color),
		Description: nullable.NewNullableWithValue(description),
	}
	resp, err := s.api.UpdateIssueTypeWithResponse(ctx, int64(id), body)
	if err != nil {
		return fmt.Errorf("update issue type: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// DeleteIssueType affects every project. The backend rejects it with 409 if any issue uses the type.
func (s *CatalogService) DeleteIssueType(ctx context.Context, id int) error {
	resp, err := s.api.DeleteIssueTypeWithResponse(ctx, int64(id))
	if err != nil {
		return fmt.Errorf("delete issue type: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// WorkflowStateCreate is a brand-new state. Its client-assigned TempKey is what transitions reference.
type WorkflowStateCreate struct {
	TempKey     string
	Name        string
	Description string
	Color       string
	Category    string
}

type WorkflowTransitionCreate struct {
	Name          string
	Description   string
	SourceTempKey string
	TargetTempKey string
}

// CreateWorkflow creates a workflow and its whole starting graph in one call.
// The backend requires one INITIAL state, a COMPLETED state, and every state reachable from initial.
func (s *CatalogService) CreateWorkflow(
	ctx context.Context, name, color, description string, states []WorkflowStateCreate, transitions []WorkflowTransitionCreate,
) (int, error) {
	body := client.CreateWorkflowJSONRequestBody{
		Name:  name,
		Color: client.CreateWorkflowRequestColor(color),
	}
	if description != "" {
		body.Description = &description
	}
	for _, st := range states {
		r := client.CreateStatusRequest{
			TempKey:  st.TempKey,
			Name:     st.Name,
			Color:    client.CreateStatusRequestColor(st.Color),
			Category: client.CreateStatusRequestCategory(st.Category),
		}
		if st.Description != "" {
			ds := st.Description
			r.Description = &ds
		}
		body.CreateStatusRequests = append(body.CreateStatusRequests, r)
	}
	for _, tr := range transitions {
		r := client.CreateTransitionRequest{
			Name:          tr.Name,
			SourceTempKey: tr.SourceTempKey,
			TargetTempKey: tr.TargetTempKey,
		}
		if tr.Description != "" {
			ds := tr.Description
			r.Description = &ds
		}
		body.CreateTransitionRequests = append(body.CreateTransitionRequests, r)
	}
	resp, err := s.api.CreateWorkflowWithResponse(ctx, body)
	if err != nil {
		return 0, fmt.Errorf("create workflow: %w", err)
	}
	if resp.StatusCode() < 200 || resp.StatusCode() >= 300 {
		return 0, newAPIError(resp.StatusCode(), resp.Body)
	}
	if resp.JSON201 != nil {
		return derefInt64(resp.JSON201.WorkflowId), nil
	}
	return 0, nil
}

// UpdateWorkflow edits the workflow's own metadata. An empty color is left out of the PATCH so the
// workflow keeps its current color. The TUI no longer edits it.
func (s *CatalogService) UpdateWorkflow(ctx context.Context, workflowID int, name, color, description string) error {
	body := client.UpdateWorkflowJSONRequestBody{
		Name:        nullable.NewNullableWithValue(name),
		Description: nullable.NewNullableWithValue(description),
	}
	if color != "" {
		body.Color = nullable.NewNullableWithValue(color)
	}
	resp, err := s.api.UpdateWorkflowWithResponse(ctx, int64(workflowID), body)
	if err != nil {
		return fmt.Errorf("update workflow: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// DeleteWorkflow is rejected with 409 if any issue type still references it.
func (s *CatalogService) DeleteWorkflow(ctx context.Context, id int) error {
	resp, err := s.api.DeleteWorkflowWithResponse(ctx, int64(id))
	if err != nil {
		return fmt.Errorf("delete workflow: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

type PositionSummary struct {
	ID          int
	Name        string
	Description string
	Color       string // ColorType enum name
}

func (s *CatalogService) ListIssueTypes(ctx context.Context) ([]IssueTypeSummary, error) {
	resp, err := s.api.ListIssueTypesWithResponse(ctx)
	if err != nil {
		return nil, fmt.Errorf("list issue types: %w", err)
	}
	if resp.JSON200 == nil {
		return nil, newAPIError(resp.StatusCode(), resp.Body)
	}
	out := make([]IssueTypeSummary, 0, len(*resp.JSON200))
	for _, t := range *resp.JSON200 {
		hierarchy := ""
		if t.Hierarchy != nil {
			hierarchy = string(*t.Hierarchy)
		}
		color := ""
		if t.Color != nil {
			color = string(*t.Color)
		}
		out = append(out, IssueTypeSummary{
			ID:             derefInt64(t.Id),
			Name:           deref(t.Name),
			Description:    deref(t.Description),
			Hierarchy:      hierarchy,
			WorkflowName:   deref(t.WorkflowName),
			Color:          color,
			SystemProvided: derefBool(t.SystemProvided),
		})
	}
	return out, nil
}

func (s *CatalogService) ListWorkflows(ctx context.Context) ([]WorkflowSummary, error) {
	resp, err := s.api.ListWorkflowsWithResponse(ctx)
	if err != nil {
		return nil, fmt.Errorf("list workflows: %w", err)
	}
	if resp.JSON200 == nil {
		return nil, newAPIError(resp.StatusCode(), resp.Body)
	}
	out := make([]WorkflowSummary, 0, len(*resp.JSON200))
	for _, w := range *resp.JSON200 {
		out = append(out, WorkflowSummary{
			ID:             derefInt64(w.Id),
			Name:           deref(w.Name),
			Description:    deref(w.Description),
			SystemProvided: derefBool(w.IsSystemProvided),
		})
	}
	return out, nil
}

func (s *CatalogService) ListPositions(ctx context.Context) ([]PositionSummary, error) {
	resp, err := s.api.ListPositionsWithResponse(ctx)
	if err != nil {
		return nil, fmt.Errorf("list positions: %w", err)
	}
	if resp.JSON200 == nil {
		return nil, newAPIError(resp.StatusCode(), resp.Body)
	}
	out := make([]PositionSummary, 0, len(*resp.JSON200))
	for _, p := range *resp.JSON200 {
		color := ""
		if p.Color != nil {
			color = string(*p.Color)
		}
		out = append(out, PositionSummary{
			ID:          derefInt64(p.Id),
			Name:        deref(p.Name),
			Description: deref(p.Description),
			Color:       color,
		})
	}
	return out, nil
}

// SetMyPosition is self-service, no admin required. A nil positionID clears it.
// Teams have no equivalent. Team assignment is admin-only.
func (s *CatalogService) SetMyPosition(ctx context.Context, positionID *int) error {
	body := client.UpdateMemberPositionRequest{}
	if positionID != nil {
		id := int64(*positionID)
		body.PositionId = &id
	}
	resp, err := s.api.UpdateMemberPositionWithResponse(ctx, body)
	if err != nil {
		return fmt.Errorf("set my position: %w", err)
	}
	if resp.StatusCode() >= 300 {
		return newAPIError(resp.StatusCode(), resp.Body)
	}
	return nil
}
