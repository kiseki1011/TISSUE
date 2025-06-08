/**
 * TISSUE Terminal System
 * 완전한 명령어 기반 터미널 인터페이스
 */
class TissueTerminal {
  constructor() {
    // 시스템 상태
    this.isInitialized = false;
    this.isDestroyed = false;
    this.bootCompleted = false;

    // DOM 요소들
    this.terminalScreen = null;
    this.terminalHistory = null;
    this.currentPrompt = null;
    this.currentInput = null;
    this.terminalCursor = null;
    this.focusKeeper = null;

    // 입력 상태
    this.currentInputText = "";
    this.commandHistory = [];
    this.historyIndex = -1;

    // 시스템 설정
    this.promptPrefix = "guest@tissue:~$ ";
    this.systemName = "TISSUE Terminal";

    // 서버 데이터 로드
    this.loadServerConfig();

    // 초기화 시작
    this.initialize();
  }

  /**
   * 서버 설정 로드
   */
  loadServerConfig() {
    const config = window.TISSUE_CONFIG || {};
    this.systemInfo = config.systemInfo || {
      version: "1.0.0",
      repository: "github.com/your-username/tissue",
      author: "Your Name",
      email: "your.email@example.com",
      license: "MIT",
      documentation: "tissue.docs.example.com",
    };

    console.log("TISSUE Terminal: Config loaded", this.systemInfo);
  }

  /**
   * 시스템 초기화
   */
  async initialize() {
    try {
      console.log("TISSUE Terminal: Initializing...");

      // DOM이 준비될 때까지 대기
      if (document.readyState === "loading") {
        await new Promise((resolve) => {
          document.addEventListener("DOMContentLoaded", resolve);
        });
      }

      // DOM 요소 설정
      this.setupDOMElements();

      // 이벤트 리스너 설정
      this.setupEventListeners();

      // 부팅 완료
      this.bootCompleted = true;
      this.isInitialized = true;

      // 포커스 설정
      this.maintainFocus();

      // 자동으로 banner 명령어 실행 (초기 환영 메시지)
      await this.executeCommand("banner");

      console.log("TISSUE Terminal: Ready");
    } catch (error) {
      console.error("TISSUE Terminal: Initialization failed", error);
      this.showCriticalError("System initialization failed");
    }
  }

  /**
   * DOM 요소 설정
   */
  setupDOMElements() {
    this.terminalScreen = document.getElementById("terminal-screen");
    this.terminalHistory = document.getElementById("terminal-history");
    this.currentPrompt = document.getElementById("current-prompt");
    this.currentInput = document.getElementById("current-input");
    this.terminalCursor = document.getElementById("terminal-cursor");
    this.focusKeeper = document.getElementById("focus-keeper");

    if (!this.terminalScreen) {
      throw new Error("Terminal screen element not found");
    }

    // 포커스 키퍼 설정
    if (this.focusKeeper) {
      this.focusKeeper.addEventListener("blur", () => {
        setTimeout(() => this.maintainFocus(), 10);
      });
    }
  }

  /**
   * 이벤트 리스너 설정
   */
  setupEventListeners() {
    // 전역 키보드 이벤트
    document.addEventListener("keydown", (e) => this.handleKeyPress(e), true);

    // 전역 클릭 이벤트 - 어디를 클릭해도 터미널에 포커스
    document.addEventListener("click", () => this.maintainFocus());

    // 윈도우 포커스 이벤트
    window.addEventListener("focus", () => this.maintainFocus());

    // 페이지 언로드 시 정리
    window.addEventListener("beforeunload", () => this.cleanup());

    // 복사/붙여넣기 지원
    document.addEventListener("paste", (e) => this.handlePaste(e));

    // 터미널 화면 클릭 시 포커스 유지
    if (this.terminalScreen) {
      this.terminalScreen.addEventListener("click", (e) => {
        e.preventDefault();
        this.maintainFocus();
      });
    }
  }

  /**
   * 키 입력 처리
   */
  handleKeyPress(event) {
    if (!this.bootCompleted) return;

    // 기본 동작 방지
    event.preventDefault();

    if (event.key === "Enter") {
      this.processCommand();
    } else if (event.key === "Backspace") {
      this.handleBackspace();
    } else if (event.key === "ArrowUp") {
      this.navigateHistory(-1);
    } else if (event.key === "ArrowDown") {
      this.navigateHistory(1);
    } else if (event.ctrlKey && event.key.toLowerCase() === "l") {
      this.executeCommand("clear");
    } else if (event.ctrlKey && event.key.toLowerCase() === "c") {
      this.handleCancel();
    } else if (event.key === "Tab") {
      this.handleTabCompletion();
    } else if (
      event.key.length === 1 &&
      !event.ctrlKey &&
      !event.altKey &&
      !event.metaKey
    ) {
      this.addCharacterToInput(event.key);
    }
  }

  /**
   * 명령어 처리
   */
  async processCommand() {
    const command = this.currentInputText.trim();

    // 명령어를 히스토리에 추가
    this.addCommandToHistory(command);

    if (command) {
      // 명령어 히스토리에 저장
      this.commandHistory.unshift(command);
      if (this.commandHistory.length > 100) {
        this.commandHistory.pop();
      }
    }

    this.historyIndex = -1;
    this.currentInputText = "";
    this.updateInputDisplay();

    // 명령어 실행
    if (command) {
      await this.executeCommand(command);
    }
  }

  /**
   * 명령어 실행
   */
  async executeCommand(commandText) {
    const [commandName, ...args] = commandText.split(" ");
    const command = this.commands[commandName];

    if (command) {
      try {
        const result = await command.call(this, args);
        if (result) {
          this.addHistoryLine(result, "history-output");
        }
      } catch (error) {
        console.error(`Command execution failed: ${commandName}`, error);
        this.addHistoryLine(
          `Error executing command: ${error.message}`,
          "error-msg"
        );
      }
    } else {
      this.addHistoryLine(`${commandName}: command not found`, "error-msg");
      this.addHistoryLine(
        `Type 'help' to see available commands.`,
        "system-msg"
      );
    }

    this.addHistoryLine("", ""); // 빈 줄 추가
  }

  /**
   * 사용 가능한 명령어들
   */
  commands = {
    // 베너 출력 명령어
    banner: function () {
      this.displayBanner();
      return null; // displayBanner가 직접 출력하므로 추가 반환값 없음
    },

    // 화면 지우기 명령어
    clear: function () {
      this.clearTerminal();
      return null;
    },

    // 도움말 명령어
    help: function () {
      const commandList = Object.keys(this.commands).sort();
      const helpText = [
        "Available commands:",
        "",
        ...commandList.map(
          (cmd) => `  ${cmd.padEnd(12)} - ${this.getCommandDescription(cmd)}`
        ),
        "",
        "Use Ctrl+L to clear screen, Ctrl+C to cancel input.",
        "Use Tab for command completion, Up/Down arrows for command history.",
      ].join("\n");

      return helpText;
    },

    // 시스템 정보 명령어
    info: function () {
      this.displaySystemInfo();
      return null;
    },

    // 버전 정보 명령어
    version: function () {
      return `TISSUE Terminal v${this.systemInfo.version}`;
    },

    // 현재 시간 명령어
    date: function () {
      return new Date().toLocaleString();
    },

    // 에코 명령어
    echo: function (args) {
      return args.join(" ");
    },

    // 사용자 정보 명령어
    whoami: function () {
      return "guest";
    },

    // 종료 명령어
    exit: function () {
      this.addHistoryLine("Goodbye!", "success-msg");
      setTimeout(() => {
        window.location.href = "/";
      }, 1000);
      return null;
    },
  };

  /**
   * 명령어 설명 반환
   */
  getCommandDescription(commandName) {
    const descriptions = {
      banner: "Display system banner and information",
      clear: "Clear the terminal screen",
      help: "Show this help message",
      info: "Display system information",
      version: "Show version information",
      date: "Display current date and time",
      echo: "Echo the given text",
      whoami: "Display current user",
      exit: "Exit the terminal",
    };
    return descriptions[commandName] || "No description available";
  }

  /**
   * 베너 출력
   */
  displayBanner() {
    // ASCII 아트 베너
    const bannerAscii = `████████╗██╗███████╗███████╗██╗   ██╗███████╗
╚══██╔══╝██║██╔════╝██╔════╝██║   ██║██╔════╝
   ██║   ██║███████╗███████╗██║   ██║█████╗
   ██║   ██║╚════██║╚════██║██║   ██║██╔══╝
   ██║   ██║███████║███████║╚██████╔╝███████╗
   ╚═╝   ╚═╝╚══════╝╚══════╝ ╚═════╝ ╚══════╝`;

    // 베너 컨테이너 생성
    const bannerContainer = document.createElement("div");
    bannerContainer.className = "banner-container";

    // ASCII 아트 요소
    const bannerElement = document.createElement("pre");
    bannerElement.className = "ascii-banner";
    bannerElement.textContent = bannerAscii;

    // 버전 정보 요소
    const versionElement = document.createElement("span");
    versionElement.className = "version-info";
    versionElement.textContent = ` ver${this.systemInfo.version}`;

    // 베너 요소들을 컨테이너에 추가
    bannerContainer.appendChild(bannerElement);
    bannerContainer.appendChild(versionElement);

    // 터미널 히스토리에 추가
    this.terminalHistory.appendChild(bannerContainer);

    // 시스템 정보 출력
    this.displaySystemInfo();

    // 도움말 메시지
    this.addHistoryLine("Type 'help' to see the list of commands.", "help-msg");
    this.addHistoryLine("", "");

    this.scrollToBottom();
  }

  /**
   * 시스템 정보 출력
   */
  displaySystemInfo() {
    const infoContainer = document.createElement("div");
    infoContainer.className = "system-info-container";

    // 제목
    const title = document.createElement("div");
    title.className = "system-info-title";
    title.textContent = "Terminal Issue Management & Collaboration";
    infoContainer.appendChild(title);

    // 빈 줄
    const emptyLine = document.createElement("div");
    emptyLine.className = "system-info-line";
    emptyLine.innerHTML = "&nbsp;";
    infoContainer.appendChild(emptyLine);

    // 시스템 정보 항목들
    const infoItems = [
      { label: "Repository:", value: this.systemInfo.repository },
      {
        label: "Author:",
        value: `${this.systemInfo.author} <${this.systemInfo.email}>`,
      },
      { label: "License:", value: this.systemInfo.license },
      { label: "Documentation:", value: this.systemInfo.documentation },
    ];

    infoItems.forEach((item) => {
      const line = document.createElement("div");
      line.className = "system-info-line";

      const label = document.createElement("span");
      label.className = "info-label";
      label.textContent = item.label;

      const value = document.createElement("span");
      value.className = "info-value";
      value.textContent = item.value;

      line.appendChild(label);
      line.appendChild(value);
      infoContainer.appendChild(line);
    });

    this.terminalHistory.appendChild(infoContainer);
    this.scrollToBottom();
  }

  /**
   * 터미널 화면 지우기
   */
  clearTerminal() {
    if (this.terminalHistory) {
      this.terminalHistory.innerHTML = "";
    }
  }

  /**
   * 명령어를 히스토리에 추가
   */
  addCommandToHistory(command) {
    const line = document.createElement("div");
    line.className = "history-line";

    const prompt = document.createElement("span");
    prompt.className = "history-prompt";
    prompt.textContent = this.promptPrefix;

    const commandSpan = document.createElement("span");
    commandSpan.className = "history-command";
    commandSpan.textContent = command;

    line.appendChild(prompt);
    line.appendChild(commandSpan);

    this.terminalHistory.appendChild(line);
    this.scrollToBottom();
  }

  /**
   * 유틸리티 메서드들
   */

  /**
   * 히스토리 라인 추가
   */
  addHistoryLine(text, className = "history-output") {
    if (!this.terminalHistory) return;

    const line = document.createElement("div");
    line.className = `history-line ${className}`;
    line.textContent = text;

    this.terminalHistory.appendChild(line);
    this.scrollToBottom();
  }

  /**
   * 입력에 문자 추가
   */
  addCharacterToInput(char) {
    this.currentInputText += char;
    this.updateInputDisplay();
  }

  /**
   * 백스페이스 처리
   */
  handleBackspace() {
    if (this.currentInputText.length > 0) {
      this.currentInputText = this.currentInputText.slice(0, -1);
      this.updateInputDisplay();
    }
  }

  /**
   * 명령어 히스토리 탐색
   */
  navigateHistory(direction) {
    if (this.commandHistory.length === 0) return;

    this.historyIndex += direction;

    if (this.historyIndex < -1) {
      this.historyIndex = -1;
    } else if (this.historyIndex >= this.commandHistory.length) {
      this.historyIndex = this.commandHistory.length - 1;
    }

    if (this.historyIndex === -1) {
      this.currentInputText = "";
    } else {
      this.currentInputText = this.commandHistory[this.historyIndex];
    }

    this.updateInputDisplay();
  }

  /**
   * 탭 완성
   */
  handleTabCompletion() {
    const input = this.currentInputText.trim();
    if (!input) return;

    const commands = Object.keys(this.commands);
    const matches = commands.filter((cmd) => cmd.startsWith(input));

    if (matches.length === 1) {
      this.currentInputText = matches[0] + " ";
      this.updateInputDisplay();
    } else if (matches.length > 1) {
      this.addHistoryLine("", "");
      this.addHistoryLine("Available completions:", "info-msg");
      this.addHistoryLine(matches.join("  "), "system-msg");
      this.addHistoryLine("", "");
    }
  }

  /**
   * 취소 처리 (Ctrl+C)
   */
  handleCancel() {
    if (this.currentInputText) {
      this.addCommandToHistory(this.currentInputText + "^C");
      this.currentInputText = "";
      this.updateInputDisplay();
    }
  }

  /**
   * 입력 표시 업데이트
   */
  updateInputDisplay() {
    if (!this.currentInput) return;
    this.currentInput.textContent = this.currentInputText;
    this.refreshCursor();
  }

  /**
   * 커서 새로고침
   */
  refreshCursor() {
    if (this.terminalCursor) {
      this.terminalCursor.style.animation = "none";
      this.terminalCursor.offsetHeight; // 강제 리플로우
      this.terminalCursor.style.animation = "terminalBlink 1s infinite";
    }
  }

  /**
   * 화면 스크롤을 아래로
   */
  scrollToBottom() {
    if (this.terminalScreen) {
      this.terminalScreen.scrollTop = this.terminalScreen.scrollHeight;
    }
  }

  /**
   * 포커스 유지
   */
  maintainFocus() {
    if (this.focusKeeper && document.activeElement !== this.focusKeeper) {
      try {
        this.focusKeeper.focus();
      } catch (error) {
        console.warn("Focus maintenance failed:", error);
      }
    }
  }

  /**
   * 붙여넣기 처리
   */
  handlePaste(event) {
    if (!this.bootCompleted) return;

    event.preventDefault();
    const pastedText = event.clipboardData.getData("text/plain");

    // 여러 줄 텍스트는 첫 번째 줄만 사용
    const singleLineText = pastedText.split("\n")[0];

    this.currentInputText += singleLineText;
    this.updateInputDisplay();
  }

  /**
   * 치명적 에러 표시
   */
  showCriticalError(message) {
    document.body.innerHTML = `
           <div style="background: #000; color: #ff0000; font-family: monospace; padding: 20px; height: 100vh;">
               <h1>TISSUE Terminal - Critical Error</h1>
               <p>${message}</p>
               <p>Please refresh the page or contact support.</p>
           </div>
       `;
  }

  /**
   * 리소스 정리
   */
  cleanup() {
    console.log("TISSUE Terminal: Cleaning up...");
    this.isDestroyed = true;
    // 이벤트 리스너들은 페이지 언로드 시 자동으로 정리됨
  }
}

// 전역 인스턴스
let terminal = null;

/**
 * 시스템 초기화
 */
document.addEventListener("DOMContentLoaded", () => {
  try {
    console.log("TISSUE Terminal: Starting system...");
    terminal = new TissueTerminal();
  } catch (error) {
    console.error("TISSUE Terminal: Critical startup failure", error);

    // 폴백 에러 화면
    document.body.innerHTML = `
           <div style="background: #000; color: #ff0000; font-family: monospace; padding: 20px; height: 100vh;">
               <h1>TISSUE Terminal - Startup Failed</h1>
               <p>The terminal system could not be initialized.</p>
               <p>Error: ${error.message}</p>
               <p><a href="/" style="color: #00ff00;">Return to homepage</a></p>
           </div>
       `;
  }
});

// 페이지 언로드 시 정리
window.addEventListener("beforeunload", () => {
  if (terminal && !terminal.isDestroyed) {
    terminal.cleanup();
  }
});

// 개발자 도구용 전역 접근
if (typeof window !== "undefined") {
  window.TISSUE_TERMINAL = terminal;
}
