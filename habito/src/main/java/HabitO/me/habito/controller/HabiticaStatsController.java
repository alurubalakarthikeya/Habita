package HabitO.me.habito.controller;

import HabitO.me.habito.dto.HabiticaStatsDTO;
import HabitO.me.habito.dto.HabiticaTaskDTO;
import HabitO.me.habito.model.User;
import HabitO.me.habito.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/stats")
public class HabiticaStatsController {

    @Autowired
    private UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    private ResponseEntity<?> checkSessionAndUser(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        Optional<User> userOptional = userRepository.findByHabiticaUserId(userId);
        if (userOptional.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        return ResponseEntity.ok(userOptional.get());
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserStats(HttpSession session) {
        ResponseEntity<?> result = checkSessionAndUser(session);
        if (!(result.getBody() instanceof User user)) return result;

        String url = "https://habitica.com/api/v3/members/" + user.getHabiticaUserId();

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-user", user.getHabiticaUserId());
        headers.set("x-api-key", user.getHabiticaApiToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JSONObject data = new JSONObject(response.getBody()).getJSONObject("data");
            JSONObject stats = data.getJSONObject("stats");
            JSONObject profile = data.optJSONObject("profile");
            String name = profile != null ? profile.optString("name", "Unknown") : "Unknown";

            HabiticaStatsDTO statsDTO = new HabiticaStatsDTO(
                    user.getHabiticaUserId(),
                    name,
                    stats.getInt("lvl"),
                    stats.getDouble("hp"),
                    stats.getDouble("mp"),
                    stats.getDouble("exp"),
                    stats.getDouble("gp"),
                    stats.getDouble("maxHealth"),
                    stats.getDouble("maxMP")
            );
            return ResponseEntity.ok(statsDTO);

        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body("Error fetching stats: " + e.getStatusText());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    @GetMapping("/tasks")
    public ResponseEntity<?> getUserTasks(HttpSession session) {
        ResponseEntity<?> result = checkSessionAndUser(session);
        if (!(result.getBody() instanceof User user)) return result;

        String url = "https://habitica.com/api/v3/tasks/user";

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-user", user.getHabiticaUserId());
        headers.set("x-api-key", user.getHabiticaApiToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            List<Map<String, Object>> allTasks = (List<Map<String, Object>>) response.getBody().get("data");

            List<HabiticaTaskDTO> taskDTOs = allTasks.stream()
                    .map(task -> new HabiticaTaskDTO(
                            (String) task.get("id"),
                            (String) task.get("text"),
                            (String) task.get("type"),
                            Boolean.TRUE.equals(task.get("completed"))
                    ))
                    .toList();

            return ResponseEntity.ok(taskDTOs);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching tasks: " + e.getMessage());
        }
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<?> completeTask(@PathVariable String taskId, HttpSession session) {
        ResponseEntity<?> result = checkSessionAndUser(session);
        if (!(result.getBody() instanceof User user)) return result;

        String url = "https://habitica.com/api/v3/tasks/" + taskId + "/score/up";

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-user", user.getHabiticaUserId());
        headers.set("x-api-key", user.getHabiticaApiToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return ResponseEntity.ok("Task marked as complete.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error completing task: " + e.getMessage());
        }
    }

    @PostMapping("/tasks/create")
    public ResponseEntity<?> createTask(@RequestBody HabiticaTaskDTO taskDTO, HttpSession session) {
        ResponseEntity<?> result = checkSessionAndUser(session);
        if (!(result.getBody() instanceof User user)) return result;

        String url = "https://habitica.com/api/v3/tasks/user";

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-user", user.getHabiticaUserId());
        headers.set("x-api-key", user.getHabiticaApiToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject newTask = new JSONObject();
        newTask.put("type", taskDTO.getType());
        newTask.put("text", taskDTO.getText());

        HttpEntity<String> entity = new HttpEntity<>(newTask.toString(), headers);

        try {
            restTemplate.postForEntity(url, entity, String.class);
            return ResponseEntity.ok("Task created successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating task: " + e.getMessage());
        }
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable String taskId, HttpSession session) {
        ResponseEntity<?> result = checkSessionAndUser(session);
        if (!(result.getBody() instanceof User user)) return result;

        String url = "https://habitica.com/api/v3/tasks/" + taskId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-user", user.getHabiticaUserId());
        headers.set("x-api-key", user.getHabiticaApiToken());

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
            return ResponseEntity.ok("Task deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting task: " + e.getMessage());
        }
    }

    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<?> updateTask(@PathVariable String taskId, @RequestBody HabiticaTaskDTO taskDTO, HttpSession session) {
        ResponseEntity<?> result = checkSessionAndUser(session);
        if (!(result.getBody() instanceof User user)) return result;

        String url = "https://habitica.com/api/v3/tasks/" + taskId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-user", user.getHabiticaUserId());
        headers.set("x-api-key", user.getHabiticaApiToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject updateTask = new JSONObject();
        updateTask.put("text", taskDTO.getText());

        HttpEntity<String> entity = new HttpEntity<>(updateTask.toString(), headers);

        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            return ResponseEntity.ok("Task updated successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating task: " + e.getMessage());
        }
    }
}