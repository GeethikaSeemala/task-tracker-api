package com.geethika.tasktracker;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties = {"app.name=Test Tracker", "app.version=9.1", "task.default-priority=LOW", "pagination.default-page-size=2"})
@AutoConfigureMockMvc
@Transactional
class TaskTrackerIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    private long create(String path, String body) throws Exception {
        var result = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
    private long project(String name) throws Exception { return create("/api/projects", "{\"name\":\"" + name + "\"}"); }
    private long user() throws Exception { return create("/api/users", "{\"name\":\"Geethika\",\"email\":\"GEETHIKA@example.com\",\"role\":\"DEVELOPER\"}"); }
    private String task(long project, Long user, String status) {
        return "{\"title\":\"Implement API\",\"projectId\":" + project + ",\"assigneeId\":" + user + ",\"status\":\"" + status + "\"}";
    }
    @Test void fullLifecycleAndReferentialConstraints() throws Exception {
        long p = project("Demo"), u = user();
        long t = create("/api/tasks", task(p, u, "TODO"));
        mvc.perform(get("/api/projects/" + p)).andExpect(status().isOk()).andExpect(jsonPath("$.createdAt").isNotEmpty());
        mvc.perform(get("/api/users/" + u)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value("geethika@example.com"));
        mvc.perform(get("/api/tasks/" + t)).andExpect(status().isOk()).andExpect(jsonPath("$.priority").value("LOW")).andExpect(jsonPath("$.assigneeId").value(u));
        mvc.perform(delete("/api/projects/" + p)).andExpect(status().isConflict());
        mvc.perform(delete("/api/users/" + u)).andExpect(status().isConflict());
        mvc.perform(put("/api/projects/" + p).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Renamed\",\"description\":\"Updated\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Renamed"));
        mvc.perform(put("/api/users/" + u).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Geethika\",\"email\":\"new@example.com\",\"role\":\"QA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("QA"));
        mvc.perform(put("/api/tasks/" + t).contentType(MediaType.APPLICATION_JSON).content(task(p, null, "DONE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DONE")).andExpect(jsonPath("$.assigneeId").isEmpty());
        mvc.perform(delete("/api/users/" + u)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/tasks/" + t)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/projects/" + p)).andExpect(status().isNoContent());
        mvc.perform(get("/api/tasks/" + t)).andExpect(status().isNotFound()).andExpect(jsonPath("$.path").value("/api/tasks/" + t));
    }
    @Test void filtersPaginatesAndSorts() throws Exception {
        long p1 = project("First"), p2 = project("Second");
        create("/api/tasks", task(p1, null, "TODO")); create("/api/tasks", task(p1, null, "DONE")); create("/api/tasks", task(p2, null, "DONE"));
        mvc.perform(get("/api/tasks")).andExpect(status().isOk()).andExpect(jsonPath("$.size").value(2)).andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.content.length()").value(2));
        mvc.perform(get("/api/tasks").param("projectId", "" + p1).param("status", "DONE")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/projects/" + p1 + "/tasks")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/projects").param("sort", "name,desc").param("size", "1")).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].name").value("Second"));
        mvc.perform(get("/api/users")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/tasks").param("page", "50")).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
    }
    @Test void returnsStructuredValidationErrors() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \",\"email\":\"bad\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.name").exists()).andExpect(jsonPath("$.fieldErrors.email").exists());
        mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Task\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.projectId").exists());
        mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void rejectsDuplicatesIncludingNormalizedEmail() throws Exception {
        project("Unique");
        mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" Unique \"}"))
                .andExpect(status().isConflict());
        user();
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Another\",\"email\":\"geethika@example.com\"}"))
                .andExpect(status().isConflict());
    }
    @Test void rejectsBadInputsAndMissingReferences() throws Exception {
        mvc.perform(get("/api/tasks").param("status", "INVALID")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/tasks").param("size", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/tasks").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/tasks").param("sort", "assignee.password")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/tasks/abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/projects/99999/tasks")).andExpect(status().isNotFound());
        mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(task(99999, null, "TODO"))).andExpect(status().isNotFound());
        long p = project("References");
        mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(task(p, 99999L, "TODO"))).andExpect(status().isNotFound());
        mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"T\",\"projectId\":"+p+",\"status\":\"BAD\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"T\",\"projectId\":"+p+",\"dueDate\":\"bad-date\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void readsExternalConfiguration() throws Exception {
        mvc.perform(get("/api/info")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Test Tracker")).andExpect(jsonPath("$.version").value("9.1"));
    }
    @Test void failedUpdateLeavesOriginalTaskIntact() throws Exception {
        long p = project("Atomic"), t = create("/api/tasks", task(p, null, "TODO"));
        mvc.perform(put("/api/tasks/"+t).contentType(MediaType.APPLICATION_JSON).content(task(p, 99999L, "DONE"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/tasks/"+t)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("TODO"));
    }
}
