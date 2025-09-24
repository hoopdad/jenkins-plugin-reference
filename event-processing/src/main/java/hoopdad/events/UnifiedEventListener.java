package hoopdad.events;

import java.io.IOException;
import java.util.logging.Logger;

import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.StageAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionListener;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.BlockEndNode;
import org.jenkinsci.plugins.workflow.graph.BlockStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

/**
 * Unified event listener for Jenkins/CloudBees jobs, pipelines, and stages.
 * - Observes job start/stop (all job types)
 * - Observes pipeline lifecycle events
 * - Detects stage start/stop within pipelines when available
 */
public class UnifiedEventListener {

    private static final Logger LOGGER = Logger.getLogger(UnifiedEventListener.class.getName());

    /**
     * === Classic Job Lifecycle (Freestyle, Pipeline, etc.) ===
     */
    @Extension
    public static class GlobalRunListener extends RunListener<Run<?,?>> {
        @Override
        public void onStarted(Run<?,?>  run, TaskListener listener) {
            LOGGER.info(() -> "onStarted JSON: ");
            // TODO put your code here
            String json = createJson(run, "onStarted");
            LOGGER.info(() -> "onStarted JSON: " + json);
        }

        @Override
        public void onCompleted(Run<?,?>  run, TaskListener listener) {
            LOGGER.info(() -> "onCompleted JSON: ");
            // TODO put your code here
            String json = createJson(run,   "OnCompleted");
            LOGGER.info(() -> "onCompleted JSON: " + json);
        }

        private String createJson(Run<?, ?> run, String event) {
            String json = "";
            try {
                String formattedTimestamp = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSX")
                    .withZone(java.time.ZoneOffset.UTC)
                    .format(java.time.Instant.ofEpochMilli(run.getStartTimeInMillis()));

                String eventGeneratedTimestamp = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSX")
                    .withZone(java.time.ZoneOffset.UTC)
                    .format(java.time.Instant.now());
                json = String.format(
                    "{\n" +
                    "  \"type\": \"job\",\n" +
                    "  \"event\": \"%s\",\n" +
                    "  \"eventGeneratedTimestamp\": \"%s\",\n" +
                    "  \"id\": \"%s\",\n" +
                    "  \"number\": \"%s\",\n" +
                    "  \"displayName\": \"%s\",\n" +
                    "  \"result\": \"%s\",\n" +
                    "  \"duration\": %d,\n" +
                    "  \"startTimeInMillis\": %d,\n" +
                    "  \"startTimestamp\": \"%s\",\n" +
                    "  \"url\": \"%s\"\n" +
                    "}",
                    event,
                    eventGeneratedTimestamp,
                    run.getId(),
                    run.getNumber(),
                    run.getDisplayName(),
                    run.getResult(),
                    run.getDuration(),
                    run.getStartTimeInMillis(),
                    formattedTimestamp,
                    run.getUrl()
                );

                // LOGGER.info(() -> "Job's parent': " + run.);
                Job j = run.getParent();
                
                LOGGER.info(() -> "getBuildStatusUrl: " + j.getBuildStatusUrl());
                j.getAllProperties().forEach(p -> {
                    try {
                        LOGGER.info(() -> "Job Property: " + p.getClass().getName() + " - " + p);
                    } catch (Exception e) {
                        LOGGER.warning("Error logging job property: " + e.getMessage());
                    }
                });
                j.getProperties().keySet().forEach(p -> {
                    try {
                        LOGGER.info(() -> "Job Property: " + p + " - " + j.getProperties().get(p));
                    } catch (Exception e) {
                        LOGGER.warning("Error logging job property: " + e.getMessage());
                    }
                });
                
                try {
                    LOGGER.info(() -> "Job's parent': " + j.getParent().getClass().getName());
                } catch (Exception e) {
                    LOGGER.warning("Error logging job property: " + e.getMessage());
                }
                try {
                    if (j.getParent() instanceof hudson.model.Hudson) {
                        hudson.model.Hudson h = (hudson.model.Hudson) j.getParent();
                        LOGGER.info(() -> "Job's parent (Hudson) getUrlChildPrefix: " + h.getUrlChildPrefix());
                        LOGGER.info(() -> "Job's parent (Hudson) getConfiguredRootUrl(): " + h.getConfiguredRootUrl());
                    } else {
                        LOGGER.info(() -> "Job's parent is not Hudson, actual type: " + j.getParent().getClass().getName());
                    }
                } catch (Exception e) {
                    LOGGER.warning("Error logging job property: " + e.getMessage());
                }

                try {
                    LOGGER.info(() -> "Job Property: " + j.getAbsoluteUrl());
                } catch (Exception e) {
                    LOGGER.warning("Error logging job property: " + e.getMessage());
                }
                try {
                    LOGGER.info(() -> "getSearchName: " + j.getApi().getSearchName());
                } catch (Exception e) {
                    LOGGER.warning("Error logging job property: " + e.getMessage());
                }
                try {
                    LOGGER.info(() -> "getSearchUrl: " + j.getApi().getSearchUrl());
                } catch (Exception e) {
                    LOGGER.warning("Error logging job property: " + e.getMessage());
                }
                try {
                    LOGGER.info(() -> "getSearch: " + j.getApi().getSearch());
                } catch (Exception e) {
                    LOGGER.warning("Error logging job property: " + e.getMessage());
                }

                LOGGER.info(() -> "Job Property: " + j.getFullName());
                LOGGER.info(() -> "Job Property: " + j.getName());


            } catch (Exception e) {
                LOGGER.warning("Error creating JSON for run: " + e.getMessage());
            }
            return json;
        }

    }

    /**
     * === Pipeline Lifecycle (Workflow start/stop/suspend) ===
     */
    @Extension
    public static class GlobalFlowExecutionListener extends FlowExecutionListener {
        @Override
        public void onRunning(FlowExecution execution) {
            // TODO put your code here
            LOGGER.info(() -> "[FlowExecutionListener] Pipeline running: " + execution);
            String json = getJson(execution, "onRunning");
            LOGGER.info(() -> "JSON: " + json);
        }

        @Override
        public void onCompleted(FlowExecution execution) {
            // TODO put your code here
            LOGGER.info(() -> "[FlowExecutionListener] Pipeline completed: " + execution);
            String json = getJson(execution, "onCompleted");
            LOGGER.info(() -> "JSON: " + json);
        }

        @Override
        public void onResumed(FlowExecution execution) {
            // TODO put your code here
            LOGGER.info(() -> "[FlowExecutionListener] Pipeline resumed: " + execution);
            String json = getJson(execution, "onResumed");
            LOGGER.info(() -> "JSON: " + json);
        }

        @Override
        public void onCreated(FlowExecution execution) {
            // TODO put your code here
            LOGGER.info(() -> "[FlowExecutionListener] Pipeline created: " + execution);
            String json = getJson(execution, "onCreated");
            LOGGER.info(() -> "JSON: " + json);
        }

        private String getJson(FlowExecution execution, String event) {
            String json = "";
            try {
                String result = "";
                String eventGeneratedTimestamp = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSX")
                    .withZone(java.time.ZoneOffset.UTC)
                    .format(java.time.Instant.now());

                json = String.format(
                    "{\n" +
                    "  \"type\": \"pipeline\",\n" +
                    "  \"event\": \"%s\",\n" +
                    "  \"eventGeneratedTimestamp\": \"%s\",\n" +
                    "  \"execution\": \"%s\",\n" +
                    // "  \"result\": \"%s\",\n" +
                    "  \"isComplete\": %s\n" +
                    "}",
                    event,
                    eventGeneratedTimestamp,
                    execution.toString(),
                    // result,
                    execution.isComplete()
                );
            } catch (Exception e) {
                LOGGER.warning("Error creating JSON for FlowExecution: " + e.getMessage());
            }
            return json;
        }

    }

}
