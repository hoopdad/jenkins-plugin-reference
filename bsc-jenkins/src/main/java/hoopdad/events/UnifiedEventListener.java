package hoopdad.events;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionListener;
import org.jenkinsci.plugins.workflow.graph.BlockEndNode;
import org.jenkinsci.plugins.workflow.graph.BlockStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowEndNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graph.FlowStartNode;

import groovyjarjarasm.asm.Label;

import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.StageAction;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Unified event listener for Jenkins/CloudBees jobs, pipelines, and stages.
 * - Observes job start/stop (all job types)
 * - Observes pipeline lifecycle events
 * - Detects stage start/stop within pipelines
 */
public class UnifiedEventListener {

    private static final Logger LOGGER = Logger.getLogger(UnifiedEventListener.class.getName());

    /**
     * === Classic Job Lifecycle (Freestyle, Pipeline, etc.) ===
     */
    @Extension
    public static class GlobalRunListener extends RunListener<Run<?,?>> {
        @Override
        public void onStarted(Run<?,?> run, TaskListener listener) {
            LOGGER.info(() -> String.format("[RunListener] Job started: %s (Build #%d)",
                    run.getFullDisplayName(), run.getNumber()));
        }

        @Override
        public void onCompleted(Run<?,?> run, TaskListener listener) {
            LOGGER.info(() -> String.format("[RunListener] Job completed: %s (Build #%d) Result=%s",
                    run.getFullDisplayName(), run.getNumber(), run.getResult()));
        }
    }

    /**
     * === Pipeline Lifecycle (Workflow start/stop/suspend) ===
     */
    @Extension
    public static class GlobalFlowExecutionListener extends FlowExecutionListener {
        @Override
        public void onRunning(FlowExecution execution) {
            LOGGER.info(() -> "[FlowExecutionListener] Pipeline running: " + execution);
        }

        @Override
        public void onCompleted(FlowExecution execution) {
            LOGGER.info(() -> "[FlowExecutionListener] Pipeline completed: " + execution);
        }

        @Override
        public void onResumed(FlowExecution execution) {
            LOGGER.info(() -> "[FlowExecutionListener] Pipeline resumed: " + execution);
        }

        @Override
        public void onCreated(FlowExecution execution) {
            LOGGER.info(() -> "[FlowExecutionListener] Pipeline created: " + execution);
        }
    }

    /**
     * === Stage Lifecycle (Start/End detection) ===
     */
    @Extension
    public static class GlobalStageGraphListener implements GraphListener {
        @Override
        public void onNewHead(FlowNode node) {
            // See https://github.com/jenkinsci/workflow-api-plugin/blob/master/src/main/java/org/jenkinsci/plugins/workflow/graph/FlowNode.java

            // if it's running,  isActive() is true
            // if it exited with an error, getError() will be non null
            //  getEnclosingId() Get the {@link #id} of the enclosing {@link BlockStartNode}for this node, or null if none. Only {@link FlowStartNode} and {@link FlowEndNode} should generally return null.
            // getId()
            // getSearchUrl() Reference from the parent {@link SearchItem} is through {@link FlowExecution#getNode(String)}
            // getDisplayFunctionName() {
            // getDisplayName() 
            // toString()
            
            // what is getExecution()   
            // how to know if we are  {@link FlowStartNode} and {@link FlowEndNode}

            if (node != null) {
                try {
                    String json = String.format(
                        "{\"id\":\"%s\",\"displayName\":\"%s\",\"displayFunctionName\":\"%s\",\"isActive\":%s,\"error\":\"%s\",\"enclosingId\":\"%s\",\"searchUrl\":\"%s\",\"url\":\"%s\"}",
                        node.getId(),
                        node.getDisplayName(),
                        node.getDisplayFunctionName(),
                        node.isActive(),
                        node.getError() != null ? node.getError().toString() : "",
                        node.getEnclosingId(),
                        node.getSearchUrl(),
                        node.getUrl()
                    );
                    LOGGER.info(json);
                } catch (IOException e) {
                    LOGGER.warning("Error getting node URL: " + e.getLocalizedMessage());
                }
            }


            LOGGER.info(() -> String.format("FlowStartNode(): %s", (node instanceof FlowStartNode)));
            LOGGER.info(() -> String.format("FlowEndNode(): %s", (node instanceof FlowEndNode)));
            FlowExecution exec = node.getExecution();
            try {LOGGER.info(exec.getCauseOfFailure().getMessage());} catch (Exception e) {}
            LOGGER.info(exec.toString());
            // Stage START
            if (node instanceof BlockStartNode) {
                String stageName = getStageName(node);
                LOGGER.info(() -> String.format("[Stage START] %s (Node %s)", stageName, node.getId()));
            }

            // Stage END
            if (node instanceof BlockEndNode) { //BlockEndNode) {
                FlowNode startNode = ((BlockEndNode) node).getStartNode();
                if (startNode instanceof FlowStartNode) {
                    String stageName = getStageName(startNode);
                    LOGGER.info(() -> String.format("[Stage END] %s (Node %s)", stageName, node.getId()));
                    String stageName2 = getStageName(node);
                    LOGGER.info(() -> String.format("[Stage END] %s (Node %s)", stageName2, node.getId()));
                }
            }
        }

        private String getStageName(FlowNode node) {
            // Fallback to LabelAction (for scripted pipelines)
            String labels = "";
            for (Action action : node.getAllActions()) {
                LOGGER.info(action.toString() );
                LOGGER.info(action.getDisplayName());
                if (action instanceof LabelAction) {
                    LabelAction stage = (LabelAction) action;
                    labels += stage.getDisplayName();
                }
            }
            if (!labels.isEmpty()) {
                return labels;
            }

            LabelAction label = node.getAction(LabelAction.class);
            if (label != null) {
                LOGGER.info(() -> String.format("[URL] %s (label.getDisplayName())", label.getUrlName(), label.getDisplayName()));
                return label.getDisplayName();
            }
            return "UnknownStage";
        }
    }
}
