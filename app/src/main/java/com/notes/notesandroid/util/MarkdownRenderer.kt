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

    private val renderer = HtmlRenderer.builder()
        .softbreak("<br />")
        .build()

    private val taskLinePattern =
        Regex("""^(\s*(?:[-*+]|\d+\.)\s+)\[( |x|X)]\s+(.*)$""")

    fun render(
        markdown: String,
        darkTheme: Boolean = true,
    ): RenderedMarkdown {
        val taskTokens = mutableListOf<TaskToken>()

        val transformedMarkdown = markdown
            .lines()
            .mapIndexed { lineIndex, line ->
                val match = taskLinePattern.matchEntire(line)

                if (match == null) {
                    line
                } else {
                    val token = "TASKTOKEN${lineIndex}X${taskTokens.size}"

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
            html = wrapHtml(body, darkTheme),
        )
    }

    fun toggleCheckbox(markdown: String, lineIndex: Int): String {
        val lines = markdown.lines().toMutableList()
        val currentLine = lines.getOrNull(lineIndex) ?: return markdown
        val match = taskLinePattern.matchEntire(currentLine) ?: return markdown

        val toggledState = if (match.groupValues[2].equals("x", ignoreCase = true)) {
            " "
        } else {
            "x"
        }

        lines[lineIndex] = "${match.groupValues[1]}[$toggledState] ${match.groupValues[3]}"

        return lines.joinToString("\n")
    }

    private fun taskHtml(task: TaskToken): String {
        val renderedInline = renderer.render(parser.parse(task.contentMarkdown))
            .trim()
            .removeWrappingParagraph()

        val stateClass = if (task.checked) {
            "task-item checked"
        } else {
            "task-item"
        }

        val checkedAttribute = if (task.checked) {
            "checked"
        } else {
            ""
        }

        return """
            <label class="$stateClass">
              <input 
                type="checkbox" 
                $checkedAttribute 
                onchange="toggleTask(${task.lineIndex})" 
              />
              <span class="task-copy">$renderedInline</span>
            </label>
        """.trimIndent()
    }

    private fun String.removeWrappingParagraph(): String {
        val value = trim()

        return if (value.startsWith("<p>") && value.endsWith("</p>")) {
            value.removePrefix("<p>").removeSuffix("</p>")
        } else {
            value
        }
    }

    private fun wrapHtml(
        body: String,
        darkTheme: Boolean,
    ): String {
        val cssVariables = if (darkTheme) {
            """
                --background: transparent;
                --surface: #25131a;
                --surface-code: #1b0f14;
                --text: #f7efe2;
                --heading: #f1e7d7;
                --strong: #f3eadb;
                --muted: #c9b59b;
                --accent: #d9b567;
                --accent-soft: rgba(217, 181, 103, 0.12);
                --border: rgba(217, 181, 103, 0.18);
            """.trimIndent()
        } else {
            """
                --background: transparent;
                --surface: #f7efe4;
                --surface-code: #f0e6d9;
                --text: #4b4748;
                --heading: #454143;
                --strong: #413d3f;
                --muted: #6a6366;
                --accent: #7b617e;
                --accent-soft: rgba(123, 97, 126, 0.10);
                --border: rgba(123, 97, 126, 0.16);
            """.trimIndent()
        }

        return """
            <!doctype html>
            <html>
              <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />

                <style>
                  :root {
                    color-scheme: ${if (darkTheme) "dark" else "light"};

                    $cssVariables
                  }

                  html, body {
                    margin: 0;
                    padding: 0;
                    background: transparent !important;
                  }

                  body {
                    font-family: sans-serif;
                    color: var(--text);
                    padding: 12px;
                    line-height: 1.6;
                    font-size: 16px;
                  }

                  p {
                    margin: 0 0 12px 0;
                  }

                  h1, h2, h3, h4, h5, h6 {
                    color: var(--heading);
                    margin: 18px 0 10px 0;
                    line-height: 1.25;
                  }

                  strong {
                    color: var(--strong);
                  }

                  a {
                    color: var(--accent);
                    text-decoration: none;
                  }

                  ul, ol {
                    padding-left: 24px;
                  }

                  li {
                    margin: 6px 0;
                  }

                  blockquote {
                    border-left: 4px solid var(--accent);
                    margin: 12px 0;
                    padding: 8px 12px;
                    color: var(--muted);
                    background: var(--accent-soft);
                    border-radius: 10px;
                  }

                  code {
                    background: var(--surface-code);
                    color: var(--text);
                    border: 1px solid var(--border);
                    border-radius: 7px;
                    padding: 2px 6px;
                    font-family: monospace;
                    font-size: 0.92em;
                  }

                  pre {
                    background: var(--surface-code);
                    color: var(--text);
                    border: 1px solid var(--border);
                    border-radius: 14px;
                    padding: 12px;
                    overflow-x: auto;
                    margin: 12px 0;
                  }

                  pre code {
                    display: block;
                    background: transparent;
                    border: none;
                    border-radius: 0;
                    padding: 0;
                    color: inherit;
                    white-space: pre;
                  }

                  .task-item {
                    display: flex;
                    align-items: flex-start;
                    gap: 10px;
                    padding: 10px 12px;
                    margin: 6px 0;
                    background: var(--accent-soft);
                    border: 1px solid var(--border);
                    border-radius: 14px;
                    cursor: pointer;
                  }

                  .task-item input {
                    margin-top: 3px;
                    width: 18px;
                    height: 18px;
                    flex-shrink: 0;
                    accent-color: var(--accent);
                  }

                  .task-item.checked .task-copy {
                    text-decoration: line-through;
                    opacity: 0.7;
                  }

                  .task-copy {
                    flex: 1;
                    min-width: 0;
                    color: var(--text);
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
    }
}
