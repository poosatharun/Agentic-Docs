package com.agentic.docs.sample.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Customer Support API — tickets, escalations, SLA tracking, agents, and knowledge base.
 */
@RestController
@RequestMapping("/api/v1/support")
@Tag(name = "Customer Support")
public class SupportController {

    @Operation(summary = "Create a support ticket. Request body: { customerId, subject, description, priority: LOW|MEDIUM|HIGH|CRITICAL, category, orderId }")
    @PostMapping("/tickets")
    public ResponseEntity<Map<String, Object>> createTicket(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(201).body(Map.of(
                "ticketId", "TKT-" + System.currentTimeMillis(),
                "status", "OPEN",
                "priority", request.getOrDefault("priority", "MEDIUM"),
                "assignedQueue", "GENERAL"
        ));
    }

    @Operation(summary = "Get ticket details by ticketId including conversation history")
    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<Map<String, Object>> getTicket(@PathVariable String ticketId) {
        return ResponseEntity.ok(Map.of(
                "ticketId", ticketId,
                "status", "IN_PROGRESS",
                "priority", "HIGH",
                "assignedAgent", "agent_042",
                "messages", List.of(
                        Map.of("from", "CUSTOMER", "message", "My order hasn't arrived", "timestamp", "2026-04-28T10:00:00Z")
                )
        ));
    }

    @Operation(summary = "Add a reply to a support ticket. Request body: { message, attachments, isInternal (agent note) }")
    @PostMapping("/tickets/{ticketId}/reply")
    public ResponseEntity<Map<String, Object>> replyToTicket(
            @PathVariable String ticketId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("ticketId", ticketId, "messageId", UUID.randomUUID().toString(), "sent", true));
    }

    @Operation(summary = "Update ticket status. Request body: { status: OPEN|IN_PROGRESS|WAITING|RESOLVED|CLOSED, resolution }")
    @PatchMapping("/tickets/{ticketId}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable String ticketId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("ticketId", ticketId, "status", request.getOrDefault("status", "OPEN")));
    }

    @Operation(summary = "Escalate a ticket to a senior agent or manager. Request body: { reason, escalateTo: TIER2|MANAGER|ENGINEERING }")
    @PostMapping("/tickets/{ticketId}/escalate")
    public ResponseEntity<Map<String, Object>> escalateTicket(
            @PathVariable String ticketId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "ticketId", ticketId,
                "escalatedTo", request.getOrDefault("escalateTo", "TIER2"),
                "newPriority", "CRITICAL"
        ));
    }

    @Operation(summary = "List tickets for a customer. Query params: customerId, status, priority, page, size")
    @GetMapping("/tickets")
    public ResponseEntity<Map<String, Object>> listTickets(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(Map.of("tickets", List.of(), "total", 0));
    }

    @Operation(summary = "Get support SLA metrics — first response time, resolution time, breach rate. Query params: fromDate, toDate, agentId")
    @GetMapping("/sla/metrics")
    public ResponseEntity<Map<String, Object>> getSlaMetrics(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String agentId) {
        return ResponseEntity.ok(Map.of(
                "avgFirstResponseMinutes", 14,
                "avgResolutionHours", 6.4,
                "slaMet", "91.2%",
                "breached", 47
        ));
    }

    @Operation(summary = "Search the knowledge base for self-service articles. Query params: q, category, page, size")
    @GetMapping("/knowledge-base/search")
    public ResponseEntity<Map<String, Object>> searchKnowledgeBase(
            @RequestParam String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(Map.of("articles", List.of(), "total", 0, "query", q));
    }

    @Operation(summary = "Create or update a knowledge base article. Request body: { title, body, category, tags, published }")
    @PutMapping("/knowledge-base/articles/{articleId}")
    public ResponseEntity<Map<String, Object>> upsertArticle(
            @PathVariable String articleId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("articleId", articleId, "saved", true, "published", request.getOrDefault("published", false)));
    }

    @Operation(summary = "Get agent performance summary — tickets handled, avg rating, resolution rate. Query params: agentId, fromDate, toDate")
    @GetMapping("/agents/{agentId}/performance")
    public ResponseEntity<Map<String, Object>> getAgentPerformance(
            @PathVariable String agentId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        return ResponseEntity.ok(Map.of(
                "agentId", agentId,
                "ticketsHandled", 342,
                "avgRating", 4.7,
                "resolutionRate", "88.5%",
                "avgHandleTimeMinutes", 22
        ));
    }
}
