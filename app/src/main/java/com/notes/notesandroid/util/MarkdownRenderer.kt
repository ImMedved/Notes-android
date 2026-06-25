package com.notes.notesandroid.util

import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

object MarkdownRenderer {
    data class RenderedMarkdown(
        val html: String,
    )

    private data class TaskToken(
        val lineIndex: Int,
        val checked: Boolean,
        val token: String,
        val contentMarkdown: String,
    )

    private val parser = Parser.builder().build()
    private val renderer = HtmlRenderer.builder().softbreak("<br />").build()
    private val taskLinePattern = Regex("""^(\s*(?:[-*+]|\d+\.)\s+)\[( |x|X)\]\s+(.*)$""")

    fun render(markdown: String): RenderedMarkdown {
        val taskTokens = mutableListOf<TaskToken>()
        val transformedMarkdown = markdown
            .lines()
            .mapIndexed { lineIndex, line ->
                val match = taskLinePattern.matchEntire(line)
                if (match == null) {
                    line
                } else {
                    val token = "__TASK_TOKEN_${lineIndex}_${taskTokens.size}__"
                    taskTokens += TaskToken(
                        lineIndex = lineIndex,
                        checked = match.groupValues[2].equals("x", ignoreCase = true),
                        token = token,
                        contentMarkdown = match.groupValues[3],
                    )
                    "${match.groupValues[1]}$token"
                }
            }
            .joinToString("\n")

        var body = renderer.render(parser.parse(transformedMarkdown))
        taskTokens.forEach { task ->
            body = body.replace(task.token, taskHtml(task))
        }

        return RenderedMarkdown(
            html = """
                <html>
                  <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                    <style>
                      :root {
                        color-scheme: light dark;
                        --surface: #efe5d4;
                        --text: #1f1f1b;
                        --muted: #5f5a4e;
                        --accent: #8d5f2f;
                        --accent-soft: rgba(141, 95, 47, 0.12);
                        --border: rgba(141, 95, 47, 0.18);
                      }

                      @media (prefers-color-scheme: dark) {
                        :root {
                          --surface: #25131a;
                          --text: #f7efe2;
                          --muted: #c9b59b;
                          --accent: #d9b567;
                          --accent-soft: rgba(217, 181, 103, 0.12);
                          --border: rgba(217, 181, 103, 0.18);
                        }
                      }

                      html, body {
                        background: transparent !important;
                      }

                      body {
                        font-family: sans-serif;
                        color: var(--text);
                        padding: 12px;
                        line-height: 1.6;
                      }

                      pre, code {
                        background: var(--surface);
                        border-radius: 8px;
                        padding: 2px 4px;
                      }

                      pre {
                        padding: 12px;
                        overflow-x: auto;
                      }

                      blockquote {
                        border-left: 4px solid var(--accent);
                        margin: 0;
                        padding-left: 12px;
                        color: var(--muted);
                      }

                      h1, h2, h3 {
                        color: var(--text);
                      }

                      a {
                        color: var(--accent);
                      }

                      .task-item {
                        display: flex;
                        align-items: flex-start;
                        gap: 10px;
                        padding: 10px 12px;
                        margin: 4px 0;
                        background: var(--accent-soft);
                        border: 1px solid var(--border);
                        border-radius: 14px;
                        cursor: pointer;
                      }

                      .task-item input {
                        margin-top: 3px;
                        width: 18px;
                        height: 18px;
                        accent-color: var(--accent);
                      }

                      .task-item.checked .task-copy {
                        text-decoration: line-through;
                        opacity: 0.7;
                      }

                      .task-copy p {
                        margin: 0;
                      }
                    </style>
                    <script type="text/javascript">
                      function toggleTask(lineIndex) {
                        if (window.TaskBridge && window.TaskBridge.onTaskToggled) {
                          window.TaskBridge.onTaskToggled(lineIndex);
                        }
                      }
                    </script>
                  </head>
                  <body>$body</body>
                </html>
            """.trimIndent()
        )
    }

    fun toggleCheckbox(markdown: String, lineIndex: Int): String {
        val lines = markdown.lines().toMutableList()
        val currentLine = lines.getOrNull(lineIndex) ?: return markdown
        val match = taskLinePattern.matchEntire(currentLine) ?: return markdown
        val toggledState = if (match.groupValues[2].equals("x", ignoreCase = true)) " " else "x"
        lines[lineIndex] = "${match.groupValues[1]}[$toggledState] ${match.groupValues[3]}"
        return lines.joinToString("\n")
    }

    private fun taskHtml(task: TaskToken): String {
        val renderedInline = renderer.render(parser.parse(task.contentMarkdown))
            .trim()
            .removePrefix("<p>")
            .removeSuffix("</p>")
        val stateClass = if (task.checked) "task-item checked" else "task-item"
        val checkedAttribute = if (task.checked) "checked" else ""
        return """
            <label class="$stateClass" onclick="toggleTask(${task.lineIndex})">
              <input type="checkbox" $checkedAttribute onclick="event.preventDefault(); toggleTask(${task.lineIndex});" />
              <span class="task-copy">$renderedInline</span>
            </label>
        """.trimIndent()
    }
}
