package ee.carlrobert.codegpt.codecompletions;

import static java.util.Objects.requireNonNull;

import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import ee.carlrobert.codegpt.CodeGPTBundle;
import ee.carlrobert.codegpt.actions.OpenSettingsAction;
import ee.carlrobert.codegpt.ui.OverlayUtil;
import ee.carlrobert.llm.client.openai.completion.ErrorDetails;
import ee.carlrobert.llm.completion.CompletionEventListener;
import javax.annotation.ParametersAreNonnullByDefault;
import org.jetbrains.annotations.Nullable;

@ParametersAreNonnullByDefault
class CodeCompletionEventListener implements CompletionEventListener {

  private static final Logger LOG = Logger.getInstance(CodeCompletionEventListener.class);

  private final Editor editor;
  private final int caretOffset;
  private final BackgroundableProcessIndicator progressIndicator;

  public CodeCompletionEventListener(
      Editor editor,
      int caretOffset,
      @Nullable BackgroundableProcessIndicator progressIndicator) {
    this.editor = editor;
    this.caretOffset = caretOffset;
    this.progressIndicator = progressIndicator;
  }

  @Override
  public void onComplete(StringBuilder messageBuilder) {
    if (progressIndicator != null) {
      progressIndicator.processFinish();
    }

    var editorManager = CodeGPTEditorManager.getInstance();
    editorManager.disposeEditorInlays(editor);

    var inlayText = messageBuilder.toString();
    if (!inlayText.isEmpty()) {
      ApplicationManager.getApplication().invokeLater(() ->
          CodeCompletionService.getInstance(requireNonNull(editor.getProject()))
              .addInlays(editor, caretOffset, inlayText));
    }
  }

  @Override
  public void onError(ErrorDetails error, Throwable ex) {
    LOG.error(error.getMessage(), ex);
    if (progressIndicator != null) {
      progressIndicator.processFinish();
    }
    Notifications.Bus.notify(OverlayUtil.getDefaultNotification(
            String.format(
                CodeGPTBundle.get("notification.completionError.description"),
                ex.getMessage()),
            NotificationType.ERROR)
        .addAction(new OpenSettingsAction()), editor.getProject());
  }

  @Override
  public void onCancelled(StringBuilder messageBuilder) {
    LOG.info("Completion cancelled");
    if (progressIndicator != null) {
      progressIndicator.processFinish();
    }
  }
}
