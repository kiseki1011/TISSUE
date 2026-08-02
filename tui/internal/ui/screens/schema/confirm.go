package schema

import (
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

// The delete-confirm dialog is a shared widget. These aliases keep this package's Model field, its
// message type-switches, and its constructor call unqualified. See widgets/confirm.go.

type confirmForm = widgets.ConfirmForm

type (
	confirmAcceptedMsg  = widgets.ConfirmAcceptedMsg
	confirmCancelledMsg = widgets.ConfirmCancelledMsg
)

func newConfirmForm(d deps.Deps, title, message, acceptLabel string) confirmForm {
	return widgets.NewConfirmForm(d.Styles, title, message, acceptLabel)
}
