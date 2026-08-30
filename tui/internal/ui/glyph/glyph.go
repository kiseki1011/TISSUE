// Package glyph maps named symbols to a Nerd Font glyph or a plain Unicode fallback.
package glyph

import (
	"os"
	"strings"
)

// Mode selects which form New resolves.
type Mode int

const (
	// Auto uses Nerd only when the environment advertises one.
	Auto Mode = iota
	Nerd
	Unicode
)

// ParseMode maps a config or flag value to a Mode, defaulting to Auto.
func ParseMode(value string) Mode {
	switch strings.ToLower(strings.TrimSpace(value)) {
	case "nerd":
		return Nerd
	case "unicode", "plain":
		return Unicode
	default:
		return Auto
	}
}

func ModeName(m Mode) string {
	switch m {
	case Nerd:
		return "nerd"
	case Unicode:
		return "unicode"
	default:
		return "auto"
	}
}

// Set holds the resolved symbol for each named glyph.
type Set struct {
	Connected            string
	Connecting           string
	Disconnected         string
	Check                string
	Cross                string
	Pin                  string
	Search               string
	Gear                 string
	CaretRight           string
	CaretDown            string
	Clock                string
	Bell                 string
	Comment              string
	Discussion           string
	Bookmark             string
	AddressBook          string
	Person               string
	PersonFeed           string
	Portrait             string
	UserCheck            string
	IssueClosed          string
	Tag                  string
	Trash                string
	Eye                  string
	EyeOff               string
	Warning              string
	Pen                  string
	PenSquare            string
	Edit                 string
	FilePen              string
	Copy                 string
	Save                 string
	Folder               string
	File                 string
	FileText             string
	Markdown             string
	FilePdf              string
	FileWord             string
	FileExcel            string
	FilePowerpoint       string
	FileCsv              string
	FileMedia            string
	FileZip              string
	Book                 string
	BookOpen             string
	Branch               string
	Merge                string
	PullRequest          string
	Refresh              string
	Key                  string
	Cabinet              string
	Calendar             string
	Git                  string
	People               string
	Run                  string
	Flag                 string
	FlagPlus             string
	FlagRemove           string
	CloseCircle          string
	Project              string
	Graph                string
	Yaml                 string
	Mail                 string
	MailRead             string
	Reply                string
	Goal                 string
	Home                 string
	TimeTrack            string
	Commit               string
	Stop                 string
	Robot                string
	Number               string
	Text                 string
	MultiSelect          string
	SingleSelect         string
	At                   string
	Flash                string
	Code                 string
	FileCode             string
	Verified             string
	Workflow             string
	ArrowSwitch          string
	Relation             string
	FileEdit             string
	FileCheck            string
	FileMinus            string
	FilePlus             string
	Document             string
	ArchiveCheck         string
	AccountSearch        string
	LastUpdated          string
	Priority             string
	AccountBadge         string
	Login                string
	Logout               string
	Identifier           string
	Filter               string
	ListFilter           string
	Log                  string
	Wand                 string
	Less                 string
	Send                 string
	Palette              string
	Gitlab               string
	Github               string
	Gitea                string
	Forgejo              string
	Forked               string
	Download             string
	Fire                 string
	Checkbox             string
	Cross2               string
	Star                 string
	List                 string
	LinkExternal         string
	Home2                string
	Trash2               string
	PersonAdd            string
	Share                string
	CaretDown2           string
	CaretUp              string
	AngleLeft            string
	AngleRight           string
	Upload               string
	Web                  string
	SymbolString         string
	WholeWord            string
	SymbolEnum           string
	Checklist            string
	SymbolBoolean        string
	SymbolNumeric        string
	Decimal              string
	Percent              string
	Hierarchy            string
	Stair                string
	TransitionConnection string
	Computer             string

	nerd bool // gates Or's fallback override
}

// variant pairs a Nerd glyph with its Unicode fallback, kept as escapes so editors cannot mangle them.
type variant struct{ nerd, unicode string }

var (
	// ●  ●
	connected = variant{"\u25cf", "\u25cf"}
	// ◐  ◐
	connecting = variant{"\u25d0", "\u25d0"}
	// ✕  ✕
	disconnected = variant{"\u2715", "\u2715"}
	//   ✓
	check = variant{"\uf00c", "\u2713"}
	//   ✗
	cross = variant{"\uf00d", "\u2717"}
	//   ✦
	pin = variant{"\uf08d", "\u2726"}
	//   ⌕
	search = variant{"\ue68f", "\u2315"}
	//   ⚙
	gear = variant{"\uf013", "\u2699"}
	//   ▸
	caretRight = variant{"\uf0da", "\u25b8"}
	//   ▾
	caretDown = variant{"\uf0d7", "\u25be"}
	//   ◷
	clock = variant{"\uf017", "\u25f7"}
	//   🔔
	bell = variant{"\uf49a", "\U0001f514"}
	//   💬
	comment = variant{"\uf27a", "\U0001f4ac"}
	//   🗣
	discussion = variant{"\ueac7", "\U0001f5e3"}
	//   🔖
	bookmark = variant{"\uf02e", "\U0001f516"}
	//   📇
	addressBook = variant{"\uf2ba", "\U0001f4c7"}
	//   👤
	person = variant{"\uf4ff", "\U0001f464"}
	//   🧑
	personFeed = variant{"\uf4ca", "\U0001f9d1"}
	//   ☺
	portrait = variant{"\ued19", "\u263a"}
	//   ☑
	userCheck = variant{"\uedc6", "\u2611"}
	//   ✔
	issueClosed = variant{"\uf41d", "\u2714"}
	//   🏷
	tag = variant{"\uf02b", "\U0001f3f7"}
	//   🗑
	trash = variant{"\uea81", "\U0001f5d1"}
	//   ◉
	eye = variant{"\uf06e", "\u25c9"}
	//   ⊘
	eyeOff = variant{"\uf070", "\u2298"}
	//   ⚠
	warning = variant{"\uf071", "\u26a0"}
	//   ✎
	pen = variant{"\uf01f", "\u270e"}
	//   ✎
	penSquare = variant{"\uf044", "\u270e"}
	// nf-cod-edit (codicon, uniform size — fa-pen glyphs render a size small in some fonts)
	edit = variant{"\uea73", "\u270e"}
	//   📝
	filePen = variant{"\uf05f", "\U0001f4dd"}
	//   ⧉
	copyGlyph = variant{"\uf0c5", "\u29c9"}
	//   💾
	save = variant{"\uf0c7", "\U0001f4be"}
	//   📁
	folder = variant{"\uf07b", "\U0001f4c1"}
	//   📄
	file = variant{"\uf15b", "\U0001f4c4"}
	//   📄
	fileText = variant{"\uf15c", "\U0001f4c4"}
	//   ≡
	markdown = variant{"\ueeab", "\u2261"}
	//   📄
	filePdf = variant{"\uf1c1", "\U0001f4c4"}
	//   📄
	fileWord = variant{"\ue6a5", "\U0001f4c4"}
	//   📄
	fileExcel = variant{"\ue6a6", "\U0001f4c4"}
	//   📄
	filePowerpoint = variant{"\uf1c4", "\U0001f4c4"}
	//   📄
	fileCsv = variant{"\ueefc", "\U0001f4c4"}
	//   🖼
	fileMedia = variant{"\uf40f", "\U0001f5bc"}
	//   📄
	fileZip = variant{"\uf410", "\U0001f4c4"}
	//   📗
	book = variant{"\uf02d", "\U0001f4d7"}
	//   📖
	bookOpen = variant{"\ueaa4", "\U0001f4d6"}
	//   ⎇
	branch = variant{"\uf126", "\u2387"}
	//   🔀
	merge = variant{"\uf17f", "\U0001f500"}
	//   ⇄
	pullRequest = variant{"\uf407", "\u21c4"}
	//   ↻
	refresh = variant{"\uf021", "\u21bb"}
	//   🔑
	key = variant{"\uf084", "\U0001f511"}
	//   🗄
	cabinet = variant{"\uf411", "\U0001f5c4"}
	//   📅
	calendar = variant{"\uf073", "\U0001f4c5"}
	//   ⎇
	git = variant{"\ue702", "\u2387"}
	//   👥
	people = variant{"\uf4fd", "\U0001f465"}
	// 󰜎  🏃
	run = variant{"\U000f070e", "\U0001f3c3"}
	//   ⚑
	flag = variant{"\uf024", "\u2691"}
	// 󰮚  ⚐
	flagPlus = variant{"\U000f0b9a", "\u2690"}
	// 󰮛  🏴
	flagRemove = variant{"\U000f0b9b", "\U0001f3f4"}
	// 󰅚  ⊗
	closeCircle = variant{"\U000f015a", "\u2297"}
	//   📊
	project = variant{"\ueb30", "\U0001f4ca"}
	// 󱁊  📊
	graph = variant{"\U000f104a", "\U0001f4ca"}
	//   📄
	yaml = variant{"\ue6a8", "\U0001f4c4"}
	//   ✉
	mail = variant{"\uf42f", "\u2709"}
	//   📬
	mailRead = variant{"\ueb1b", "\U0001f4ec"}
	// U+21A9 in both modes: the plain BMP hook-arrow renders the same in every font
	reply = variant{"\u21a9", "\u21a9"}
	//   🎯
	goal = variant{"\uf4de", "\U0001f3af"}
	//   ⌂
	home = variant{"\ueb06", "\u2302"}
	//   ⏱
	timeTrack = variant{"\ue641", "\u23f1"}
	//   ⊙
	commit = variant{"\ue729", "\u2299"}
	//   ⏹
	stop = variant{"\uf46e", "\u23f9"}
	//   🤖
	robot = variant{"\uee0d", "\U0001f916"}
	//   №
	number = variant{"\uf4f7", "\u2116"}
	// 󰦨  ≡
	text = variant{"\U000f09a8", "\u2261"}
	//   ☰
	multiSelect = variant{"\uf4f3", "\u2630"}
	//   ⊡
	singleSelect = variant{"\uf516", "\u22a1"}
	//   @
	at = variant{"\uf1fa", "\u0040"}
	//   ⚡
	flash = variant{"\uf0e7", "\u26a1"}
	//   ⟨
	code = variant{"\uf121", "\u27e8"}
	//   📄
	fileCode = variant{"\uf40d", "\U0001f4c4"}
	//   ✔
	verified = variant{"\uf4a1", "\u2714"}
	//   ⋔
	workflow = variant{"\uf52e", "\u22d4"}
	//   ⇆
	arrowSwitch = variant{"\uf443", "\u21c6"}
	// 󱒣  ⟷
	relation = variant{"\U000f14a3", "\u27f7"}
	// 󰷈  📝
	fileEdit = variant{"\U000f0dc8", "\U0001f4dd"}
	// 󱪙  📄
	fileCheck = variant{"\U000f1a99", "\U0001f4c4"}
	// 󱪛  📄
	fileMinus = variant{"\U000f1a9b", "\U0001f4c4"}
	// 󱪝  📄
	filePlus = variant{"\U000f1a9d", "\U0001f4c4"}
	// 󰈙  📄
	document = variant{"\U000f0219", "\U0001f4c4"}
	//   ✓
	archiveCheck = variant{"\uf187", "\u2713"}
	// 󰀖  🔍
	accountSearch = variant{"\U000f0016", "\U0001f50d"}
	// nf-fa-history, fallback clockwise arrow
	lastUpdated = variant{"\uf1da", "\u21bb"}
	// nf-md-priority (plane-15). Empty fallback so call sites pass their own text through Or.
	priority = variant{"\U000f08be", ""}
	// nf-fa-id-badge, fallback ◈
	accountBadge = variant{"\uf2c2", "\u25c8"}
	// 󰍂  →
	login = variant{"\U000f0342", "\u2192"}
	// 󰍃  ←
	logout = variant{"\U000f0343", "\u2190"}
	// 󰻾  🆔
	identifier = variant{"\U000f0efe", "\U0001f194"}
	// U+25BC in both modes: every nerd funnel/sliders glyph reads off-centre in the IntelliJ font
	filter = variant{"\u25bc", "\u25bc"}
	//   ▤
	listFilter = variant{"\ueb83", "\u25a4"}
	//   ☰
	log = variant{"\uf4ed", "\u2630"}
	//   ✧
	wand = variant{"\uebcf", "\u2727"}
	//   📄
	less = variant{"\ue60b", "\U0001f4c4"}
	//   ➤
	send = variant{"\uec0f", "\u27a4"}
	//   🎨
	palette = variant{"\uefcc", "\U0001f3a8"}
	//   ⎇
	gitlab = variant{"\ue65c", "\u2387"}
	//   ⎇
	github = variant{"\ue65b", "\u2387"}
	//   ⎇
	gitea = variant{"\uf339", "\u2387"}
	//   ⎇
	forgejo = variant{"\uf335", "\u2387"}
	//   ⑂
	forked = variant{"\uf402", "\u2442"}
	//   ⇩
	download = variant{"\uf409", "\u21e9"}
	// 󰈸  🔥
	fire = variant{"\U000f0238", "\U0001f525"}
	//   ☑
	checkbox      = variant{"\ue63f", "\u2611"}
	symbolString  = variant{"\ueb8d", "\u2261"}
	wholeWord     = variant{"\ueb7e", "\u25a5"}
	symbolEnum    = variant{"\uea95", "\u25be"}
	checklist     = variant{"\ueab3", "\u2611"}
	symbolBoolean = variant{"\uea8f", "\u25d1"}
	symbolNumeric = variant{"\uea90", "\u0023"}
	// nf-md-decimal. Fallback #, though the field-type picker overrides it to empty.
	decimal = variant{"\U000f10a1", "\u0023"}
	percent = variant{"\uf295", "\u0025"}
	//   ✗
	cross2 = variant{"\uf00d", "\u2717"}
	//   ★
	star = variant{"\uf51f", "\u2605"}
	//   ☰
	list = variant{"\uf451", "\u2630"}
	//   ↗
	linkExternal = variant{"\uf465", "\u2197"}
	//   ⌂
	home2 = variant{"\uf4e2", "\u2302"}
	// 󰩹  🗑
	trash2 = variant{"\U000f0a79", "\U0001f5d1"}
	//   👤
	personAdd = variant{"\uf4fe", "\U0001f464"}
	//   🔗
	share = variant{"\uf1e0", "\U0001f517"}
	//   ▾
	caretDown2 = variant{"\uf0d7", "\u25be"}
	//   ▴
	caretUp = variant{"\uf0d8", "\u25b4"}
	//   ‹
	angleLeft = variant{"\uf104", "\u2039"}
	//   ›
	angleRight = variant{"\uf105", "\u203a"}
	//   ⇧
	upload = variant{"\uf40a", "\u21e7"}
	// 󰖟  🌐
	web = variant{"\U000f059f", "\U0001f310"}
	// cod-type_hierarchy, fallback ↳
	hierarchy = variant{"\uebb9", "\u21b3"}
	// md-stairs (plane-15), fallback ≡
	stair = variant{"\U000f04cd", "\u2261"}
	// md-transit_connection_variant (plane-15), fallback ⇄
	transitionConnection = variant{"\U000f0d3d", "\u21c4"}
	// fa-desktop, fallback ▣
	computer = variant{"\uf108", "\u25a3"}
)

// Or applies an optional fallback override to the mode-resolved glyph g. Nerd mode always returns g.
// Fallback mode returns the override when one is supplied (an empty string counts), else g.
func (s Set) Or(g string, override ...string) string {
	if s.nerd || len(override) == 0 {
		return g
	}
	return override[0]
}

// FieldTypeGlyph returns the glyph for an IssueFieldType, empty on plain terminals so the label shows.
func (s Set) FieldTypeGlyph(fieldType string) string {
	switch fieldType {
	case "TEXT":
		return s.Or(s.SymbolString, "")
	case "SHORT_TEXT":
		return s.Or(s.WholeWord, "")
	case "SELECT_OPTION":
		return s.Or(s.SymbolEnum, "")
	case "CHECKLIST":
		return s.Or(s.Checklist, "")
	case "BOOLEAN":
		return s.Or(s.SymbolBoolean, "")
	case "DATE":
		return s.Or(s.Calendar, "")
	case "DECIMAL":
		return s.Or(s.Decimal, "")
	case "INTEGER":
		return s.Or(s.Number, "")
	case "PERCENTAGE":
		return s.Or(s.Percent, "")
	case "TIMESTAMP":
		return s.Or(s.Clock, "")
	}
	return ""
}

// New resolves every glyph for the given mode.
func New(mode Mode) Set {
	nerd := mode == Nerd || (mode == Auto && detectNerd())
	pick := func(v variant) string {
		if nerd {
			return v.nerd
		}
		return v.unicode
	}
	return Set{
		Connected:            pick(connected),
		Connecting:           pick(connecting),
		Disconnected:         pick(disconnected),
		Check:                pick(check),
		Cross:                pick(cross),
		Pin:                  pick(pin),
		Search:               pick(search),
		Gear:                 pick(gear),
		CaretRight:           pick(caretRight),
		CaretDown:            pick(caretDown),
		Clock:                pick(clock),
		Bell:                 pick(bell),
		Comment:              pick(comment),
		Discussion:           pick(discussion),
		Bookmark:             pick(bookmark),
		AddressBook:          pick(addressBook),
		Person:               pick(person),
		PersonFeed:           pick(personFeed),
		Portrait:             pick(portrait),
		UserCheck:            pick(userCheck),
		IssueClosed:          pick(issueClosed),
		Tag:                  pick(tag),
		Trash:                pick(trash),
		Eye:                  pick(eye),
		EyeOff:               pick(eyeOff),
		Warning:              pick(warning),
		Pen:                  pick(pen),
		PenSquare:            pick(penSquare),
		Edit:                 pick(edit),
		FilePen:              pick(filePen),
		Copy:                 pick(copyGlyph),
		Save:                 pick(save),
		Folder:               pick(folder),
		File:                 pick(file),
		FileText:             pick(fileText),
		Markdown:             pick(markdown),
		FilePdf:              pick(filePdf),
		FileWord:             pick(fileWord),
		FileExcel:            pick(fileExcel),
		FilePowerpoint:       pick(filePowerpoint),
		FileCsv:              pick(fileCsv),
		FileMedia:            pick(fileMedia),
		FileZip:              pick(fileZip),
		Book:                 pick(book),
		BookOpen:             pick(bookOpen),
		Branch:               pick(branch),
		Merge:                pick(merge),
		PullRequest:          pick(pullRequest),
		Refresh:              pick(refresh),
		Key:                  pick(key),
		Cabinet:              pick(cabinet),
		Calendar:             pick(calendar),
		Git:                  pick(git),
		People:               pick(people),
		Run:                  pick(run),
		Flag:                 pick(flag),
		FlagPlus:             pick(flagPlus),
		FlagRemove:           pick(flagRemove),
		CloseCircle:          pick(closeCircle),
		Project:              pick(project),
		Graph:                pick(graph),
		Yaml:                 pick(yaml),
		Mail:                 pick(mail),
		MailRead:             pick(mailRead),
		Reply:                pick(reply),
		Goal:                 pick(goal),
		Home:                 pick(home),
		TimeTrack:            pick(timeTrack),
		Commit:               pick(commit),
		Stop:                 pick(stop),
		Robot:                pick(robot),
		Number:               pick(number),
		Text:                 pick(text),
		MultiSelect:          pick(multiSelect),
		SingleSelect:         pick(singleSelect),
		At:                   pick(at),
		Flash:                pick(flash),
		Code:                 pick(code),
		FileCode:             pick(fileCode),
		Verified:             pick(verified),
		Workflow:             pick(workflow),
		ArrowSwitch:          pick(arrowSwitch),
		Relation:             pick(relation),
		FileEdit:             pick(fileEdit),
		FileCheck:            pick(fileCheck),
		FileMinus:            pick(fileMinus),
		FilePlus:             pick(filePlus),
		Document:             pick(document),
		ArchiveCheck:         pick(archiveCheck),
		AccountSearch:        pick(accountSearch),
		LastUpdated:          pick(lastUpdated),
		Priority:             pick(priority),
		AccountBadge:         pick(accountBadge),
		Login:                pick(login),
		Logout:               pick(logout),
		Identifier:           pick(identifier),
		Filter:               pick(filter),
		ListFilter:           pick(listFilter),
		Log:                  pick(log),
		Wand:                 pick(wand),
		Less:                 pick(less),
		Send:                 pick(send),
		Palette:              pick(palette),
		Gitlab:               pick(gitlab),
		Github:               pick(github),
		Gitea:                pick(gitea),
		Forgejo:              pick(forgejo),
		Forked:               pick(forked),
		Download:             pick(download),
		Fire:                 pick(fire),
		Checkbox:             pick(checkbox),
		SymbolString:         pick(symbolString),
		WholeWord:            pick(wholeWord),
		SymbolEnum:           pick(symbolEnum),
		Checklist:            pick(checklist),
		SymbolBoolean:        pick(symbolBoolean),
		SymbolNumeric:        pick(symbolNumeric),
		Decimal:              pick(decimal),
		Percent:              pick(percent),
		Hierarchy:            pick(hierarchy),
		Stair:                pick(stair),
		TransitionConnection: pick(transitionConnection),
		Computer:             pick(computer),
		Cross2:               pick(cross2),
		Star:                 pick(star),
		List:                 pick(list),
		LinkExternal:         pick(linkExternal),
		Home2:                pick(home2),
		Trash2:               pick(trash2),
		PersonAdd:            pick(personAdd),
		Share:                pick(share),
		CaretDown2:           pick(caretDown2),
		CaretUp:              pick(caretUp),
		AngleLeft:            pick(angleLeft),
		AngleRight:           pick(angleRight),
		Upload:               pick(upload),
		Web:                  pick(web),
		nerd:                 nerd,
	}
}

// detectNerd trusts only an explicit env signal: terminals do not report their font.
func detectNerd() bool {
	for _, key := range []string{"TISSUE_NERD_FONT", "NERD_FONT"} {
		switch strings.ToLower(strings.TrimSpace(os.Getenv(key))) {
		case "1", "true", "yes", "on":
			return true
		}
	}
	return false
}
