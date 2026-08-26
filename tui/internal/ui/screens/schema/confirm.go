package schema

import (
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

// Aliases keep call sites unqualified. The dialog lives in widgets/confirm.go.

type confirmForm = widgets.ConfirmForm

type (
	confirmAcceptedMsg  = widgets.ConfirmAcceptedMsg
	confirmCancelledMsg = widgets.ConfirmCancelledMsg
)

func newConfirmForm(d deps.Deps, title, message, acceptLabel string) confirmForm {
	return widgets.NewConfirmForm(d.Styles, title, message, acceptLabel)
}
