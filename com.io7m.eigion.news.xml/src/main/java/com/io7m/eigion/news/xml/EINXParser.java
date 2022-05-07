/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.eigion.news.xml;

import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A news item XML parser.
 */

public final class EINXParser
{
  private static final Pattern NEWLINES =
    Pattern.compile("\n");
  private static final Pattern WHITESPACE =
    Pattern.compile("[ ]+");

  private final DocumentBuilderFactory documents;

  /**
   * A news item XML parser.
   */

  public EINXParser()
  {
    this.documents = DocumentBuilderFactory.newDefaultInstance();
    this.documents.setNamespaceAware(false);
    this.documents.setValidating(false);
    this.documents.setXIncludeAware(false);
    this.documents.setExpandEntityReferences(false);
    this.documents.setIgnoringComments(true);
  }

  /**
   * Parse a document from the given input stream.
   *
   * @param stream The input stream
   *
   * @return A parsed document
   *
   * @throws Exception On errors
   */

  public EINXDocument parse(
    final InputStream stream)
    throws Exception
  {
    final var documentBuilder =
      this.documents.newDocumentBuilder();
    final var document =
      documentBuilder.parse(stream);

    final var root =
      document.getDocumentElement();

    final var rootTag = root.getTagName();
    if (!Objects.equals(rootTag, "NewsItem")) {
      throw new IOException("Unrecognized element name: " + rootTag);
    }

    final var topLevels =
      new ArrayList<EINXElementType>();

    final var rootNodes = root.getChildNodes();
    for (int index = 0; index < rootNodes.getLength(); ++index) {
      final var childNode = rootNodes.item(index);
      if (childNode instanceof Element element) {
        final var tagName = element.getTagName();
        switch (tagName) {
          case "Paragraph" -> {
            topLevels.add(parseParagraph(element));
          }
          default -> {
            throw new IOException("Unrecognized element name: " + tagName);
          }
        }
      }
    }

    return new EINXDocument(List.copyOf(topLevels));
  }

  private static String removeNewlines(
    final String text)
  {
    final var trimmed =
      text.trim();
    final var noNewLines =
      NEWLINES.matcher(trimmed).replaceAll("");
    final var noSpace =
      WHITESPACE.matcher(noNewLines).replaceAll(" ");

    return noSpace;
  }

  private static EINXElementType parseParagraph(
    final Element element)
    throws IOException, URISyntaxException
  {
    final var inlines = new ArrayList<EINXElementInlineType>();
    final var childNodes = element.getChildNodes();
    for (int index = 0; index < childNodes.getLength(); ++index) {
      final var childNode = childNodes.item(index);
      if (childNode instanceof Text text) {
        inlines.add(new EINXText(removeNewlines(text.getTextContent())));
        continue;
      }
      if (childNode instanceof Element e) {
        final var tagName = e.getTagName();
        switch (tagName) {
          case "Link" -> {
            inlines.add(
              new EINXHyperlink(
                removeNewlines(e.getTextContent()),
                new URI(e.getAttribute("target"))
              )
            );
          }
          default -> {
            throw new IOException("Unrecognized element name: " + tagName);
          }
        }
      }
    }
    return new EINXParagraph(List.copyOf(inlines));
  }
}
