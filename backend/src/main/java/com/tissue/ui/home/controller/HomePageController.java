package com.tissue.ui.home.controller;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomePageController {

	/**
	 * 메인 터미널 페이지
	 */
	@GetMapping("/")
	public String terminal(Model model) {
		// 터미널 방식의 페이지는 클라이언트 사이드에서 대부분 것을 처리함
		model.addAttribute("pageTitle", "TISSUE Terminal");

		return "home/terminal_index";
	}

	/**
	 * 헬스체크 엔드포인트
	 * 단순 시스템 상태 확인용 API
	 */
	@GetMapping("api/health")
	@ResponseBody
	public Map<String, Object> health() {
		// TODO: TerminalHealth라는 DTO 만들어서 사용
		Map<String, Object> health = new HashMap<>();
		health.put("status", "UP");
		health.put("timestamp", LocalDateTime.now());
		health.put("version", "1.0.0");
		return health;
	}

	/**
	 * 시스템 정보 API (동적으로 로드할 수 있도록)
	 * 베너의 시스템 정보 조회 및 표시에 사용하기 위한 API
	 */
	@GetMapping("/api/system-info")
	@ResponseBody
	public Map<String, Object> getSystemInfo() {
		// TODO: TerminalInfo라는 DTO 만들어서 사용
		// TODO: 값은 설정값 파일에서 주입
		Map<String, Object> systemInfo = new HashMap<>();
		systemInfo.put("version", "1.0.0");
		systemInfo.put("repository", "github.com/your-username/tissue");
		systemInfo.put("author", "Your Name");
		systemInfo.put("email", "your.email@example.com");
		systemInfo.put("license", "MIT");
		systemInfo.put("documentation", "tissue.docs.example.com");
		systemInfo.put("uptime", getUptime());
		return systemInfo;
	}

	/**
	 * 서버 업타임 계산
	 */
	private String getUptime() {
		long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
		long uptimeSeconds = uptimeMillis / 1000;
		long hours = uptimeSeconds / 3600;
		long minutes = (uptimeSeconds % 3600) / 60;
		long seconds = uptimeSeconds % 60;

		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}
}
