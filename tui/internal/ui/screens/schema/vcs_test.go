package schema

import (
	"strings"
	"testing"
)

// vcsModel loads the example workflow with the given VCS transition mappings.
func vcsModel(t *testing.T, opened, merged int) Model {
	t.Helper()
	m := mkWorkflowModel(t)
	d := exampleWorkflow()
	d.VcsPrOpenedTransitionID = opened
	d.VcsPrMergedTransitionID = merged
	m, _ = m.Update(WorkflowDetailLoadedMsg{ID: 1, Detail: d})
	return m
}

func lineWith(body, needle string) string {
	for _, ln := range strings.Split(body, "\n") {
		if strings.Contains(ln, needle) {
			return ln
		}
	}
	return ""
}

func TestVcsSectionShowsTransitionNames(t *testing.T) {
	m := vcsModel(t, 10, 12) // 10 = Start (PR opened), 12 = Approve (PR merged)
	body := stripANSI(m.detail())
	if !strings.Contains(body, "VCS Automation") {
		t.Fatalf("no VCS Automation section:\n%s", body)
	}
	if got := lineWith(body, "PR opened"); !strings.Contains(got, "Start") {
		t.Errorf("PR opened row = %q, want it to name the Start transition", got)
	}
	if got := lineWith(body, "PR merged"); !strings.Contains(got, "Approve") {
		t.Errorf("PR merged row = %q, want it to name the Approve transition", got)
	}
}

func TestVcsUnsetShowsDash(t *testing.T) {
	m := vcsModel(t, 0, 0)
	body := stripANSI(m.detail())
	if got := lineWith(body, "PR opened"); !strings.Contains(got, "-") {
		t.Errorf("unset PR opened row = %q, want a dash", got)
	}
}

// The VCS section needs the graph's transitions to resolve names, so it hides while loading.
func TestVcsHiddenWhileLoading(t *testing.T) {
	m := mkWorkflowModel(t)
	delete(m.wfDetail, 1)
	m.wfPending[1] = true
	if body := stripANSI(m.detail()); strings.Contains(body, "VCS Automation") {
		t.Errorf("VCS section shown before the graph loaded:\n%s", body)
	}
}
