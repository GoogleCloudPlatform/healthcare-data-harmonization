// Copyright 2022 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.cloud.verticals.foundations.dataharmonization.doclet;

import com.google.cloud.verticals.foundations.dataharmonization.doclet.formatting.MarkupFormat;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.model.ArgumentDoc;
import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.AuthorTree;
import com.sun.source.doctree.CommentTree;
import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocRootTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.EntityTree;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.IdentifierTree;
import com.sun.source.doctree.InheritDocTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.SerialDataTree;
import com.sun.source.doctree.SerialFieldTree;
import com.sun.source.doctree.SerialTree;
import com.sun.source.doctree.SinceTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import com.sun.source.doctree.ValueTree;
import com.sun.source.doctree.VersionTree;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import org.apache.commons.text.StringEscapeUtils;

/** Converts a JavaDoc doctree to a string. Ignores params, returns, and throws tags. */
public class DocTreeToString implements DocTreeVisitor<StringBuilder, StringBuilder> {
  private static final String PRE_TAG = "pre";
  private static final String PARAM_TAG = "param";
  private static final String PARAM_NAME_ATTR = "name";
  private static final String PARAM_TYPE_ATTR = "type";

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final MarkupFormat format;
  private final List<ArgumentDoc> specialArgs = new ArrayList<>();
  private final Stack<StringBuilder> tempBuilders = new Stack<>();
  private boolean isInPre = false;

  public DocTreeToString(MarkupFormat format) {
    this.format = format;
  }

  public StringBuilder visitAll(List<? extends DocTree> body, StringBuilder state) {
    for (DocTree dt : body) {
      state = dt.accept(this, state);
    }
    return state;
  }

  @Override
  public StringBuilder visitAttribute(AttributeTree node, StringBuilder state) {
    throw new UnsupportedOperationException("Attributes are not supported.");
  }

  @Override
  public StringBuilder visitAuthor(AuthorTree node, StringBuilder state) {
    throw new UnsupportedOperationException("Authors are not supported.");
  }

  @Override
  public StringBuilder visitComment(CommentTree node, StringBuilder state) {
    String body = cleanText(node.getBody());
    return state.append(format.text(body));
  }

  @Override
  public StringBuilder visitDeprecated(DeprecatedTree node, StringBuilder state) {
    state
        .append(format.paragraphBreak())
        .append(format.startBold())
        .append(format.text("DEPRECATED"))
        .append(format.endBold())
        .append(format.text(" "));
    visitAll(node.getBody(), state);
    return state.append(format.paragraphBreak());
  }

  @Override
  public StringBuilder visitDocComment(DocCommentTree node, StringBuilder state) {
    visitAll(node.getFullBody(), state);
    return visitAll(node.getBlockTags(), state);
  }

  @Override
  public StringBuilder visitDocRoot(DocRootTree node, StringBuilder state) {
    return node.accept(this, state);
  }

  @Override
  public StringBuilder visitEndElement(EndElementTree node, StringBuilder state) {
    if (node.getName().toString().toLowerCase(Locale.ROOT).equals(PARAM_TAG)) {
      ArgumentDoc current = specialArgs.get(specialArgs.size() - 1);
      specialArgs.set(
          specialArgs.size() - 1,
          ArgumentDoc.builder()
              .setName(current.name())
              .setType(current.type())
              .setBody(state.toString())
              .build());
      return tempBuilders.pop();
    } else if (node.getName().toString().toLowerCase(Locale.ROOT).equals(PRE_TAG)) {
      isInPre = false;
    }
    return state.append(MarkupFormat.mapEndTag(node.getName().toString(), format));
  }

  @Override
  public StringBuilder visitEntity(EntityTree node, StringBuilder state) {
    return state.append(format.text(StringEscapeUtils.unescapeHtml4(node.toString())));
  }

  @Override
  public StringBuilder visitErroneous(ErroneousTree node, StringBuilder state) {
    return state.append(format.text(node.getBody()));
  }

  @Override
  public StringBuilder visitIdentifier(IdentifierTree node, StringBuilder state) {
    return state.append(format.text(node.getName().toString()));
  }

  @Override
  public StringBuilder visitInheritDoc(InheritDocTree node, StringBuilder state) {
    throw new UnsupportedOperationException("Inherit doc is not supported");
  }

  @Override
  public StringBuilder visitLink(LinkTree node, StringBuilder state) {
    return state.append(format.text(node.getReference().getSignature()));
  }

  @Override
  public StringBuilder visitLiteral(LiteralTree node, StringBuilder state) {
    return state
        .append(format.startInlineCode())
        .append(format.text(node.getBody().getBody().trim()))
        .append(format.endInlineCode());
  }

  @Override
  public StringBuilder visitParam(ParamTree node, StringBuilder state) {
    StringBuilder body = new StringBuilder();
    specialArgs.add(
        ArgumentDoc.builder()
            .setName(format.text(node.getName().getName().toString()))
            .setBody(visitAll(node.getDescription(), body).toString())
            .build());
    return state;
  }

  @Override
  public StringBuilder visitReference(ReferenceTree node, StringBuilder state) {
    return state
        .append(format.startInlineCode())
        .append(format.text(node.getSignature()))
        .append(format.endInlineCode());
  }

  @Override
  public StringBuilder visitReturn(ReturnTree node, StringBuilder state) {
    return state;
  }

  @Override
  public StringBuilder visitSee(SeeTree node, StringBuilder state) {
    state.append(format.text("(see "));
    visitAll(node.getReference(), state);
    return state.append(format.text(")"));
  }

  @Override
  public StringBuilder visitSerial(SerialTree node, StringBuilder state) {
    logger.atWarning().log("Ignoring %s %s%n", node.getClass().getSimpleName(), node);
    return state;
  }

  @Override
  public StringBuilder visitSerialData(SerialDataTree node, StringBuilder state) {
    logger.atWarning().log("Ignoring %s %s%n", node.getClass().getSimpleName(), node);
    return state;
  }

  @Override
  public StringBuilder visitSerialField(SerialFieldTree node, StringBuilder state) {
    logger.atWarning().log("Ignoring %s %s%n", node.getClass().getSimpleName(), node);
    return state;
  }

  @Override
  public StringBuilder visitSince(SinceTree node, StringBuilder state) {
    state.append(format.text("(since "));
    visitAll(node.getBody(), state);
    return state.append(format.text(")"));
  }

  @Override
  public StringBuilder visitStartElement(StartElementTree node, StringBuilder state) {
    // Custom/special parameters
    if (node.getName().toString().toLowerCase(Locale.ROOT).equals(PARAM_TAG)) {
      String name = getAttributeValue(PARAM_NAME_ATTR, node.getAttributes());
      String type = getAttributeValue(PARAM_TYPE_ATTR, node.getAttributes());
      specialArgs.add(ArgumentDoc.builder().setName(name).setType(type).build());
      tempBuilders.push(state);
      return new StringBuilder();
    } else if (node.getName().toString().toLowerCase(Locale.ROOT).equals(PRE_TAG)) {
      isInPre = true;
    }
    return state.append(MarkupFormat.mapStartTag(node.getName().toString(), format));
  }

  private String getAttributeValue(String name, List<? extends DocTree> attrs) {
    return attrs.stream()
        .filter(AttributeTree.class::isInstance)
        .map(AttributeTree.class::cast)
        .filter(a -> a.getName().toString().toLowerCase(Locale.ROOT).equals(name))
        .map(a -> visitAll(a.getValue(), new StringBuilder()).toString())
        .findFirst()
        .orElse("???");
  }

  @Override
  public StringBuilder visitText(TextTree node, StringBuilder state) {
    String body = cleanText(node.getBody());
    return state.append(format.text(body));
  }

  private String cleanText(String body) {
    if (!isInPre) {
      body = body.replaceAll("\n\n ", "\n");
    } else {
      body = body.replaceAll("\n ", "\n");
    }
    body = body.replaceAll("^\n+", "").replaceAll("\n+$", "");
    return body;
  }

  @Override
  public StringBuilder visitThrows(ThrowsTree node, StringBuilder state) {
    return state;
  }

  @Override
  public StringBuilder visitUnknownBlockTag(UnknownBlockTagTree node, StringBuilder state) {
    logger.atWarning().log(
        "Unknown block tag %s, but trying to map it anyway%n", node.getTagName());
    state.append(MarkupFormat.mapStartTag(node.getTagName(), format));
    visitAll(node.getContent(), state);
    return state.append(MarkupFormat.mapEndTag(node.getTagName(), format));
  }

  @Override
  public StringBuilder visitUnknownInlineTag(UnknownInlineTagTree node, StringBuilder state) {
    logger.atWarning().log(
        "Unknown inline tag %s, but trying to map it anyway%n", node.getTagName());
    state.append(MarkupFormat.mapStartTag(node.getTagName(), format));
    visitAll(node.getContent(), state);
    return state.append(MarkupFormat.mapEndTag(node.getTagName(), format));
  }

  @Override
  public StringBuilder visitValue(ValueTree node, StringBuilder state) {
    return state.append(format.text(node.getReference().getSignature()));
  }

  @Override
  public StringBuilder visitVersion(VersionTree node, StringBuilder state) {
    state.append(format.text("version: "));
    return visitAll(node.getBody(), state);
  }

  @Override
  public StringBuilder visitOther(DocTree node, StringBuilder state) {
    logger.atWarning().log("Unknown other DocTree node %s%n", node.toString());
    return state;
  }

  public ImmutableList<ArgumentDoc> getSpecialArgs() {
    return ImmutableList.copyOf(specialArgs);
  }
}
