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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.AuthorTree;
import com.sun.source.doctree.CommentTree;
import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.lang.model.element.Name;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for doc to string. */
@RunWith(JUnit4.class)
public class DocTreeToStringTest {
  private final DocTreeToString doctree = new DocTreeToString(new TestMarkup());

  @Test
  public void visitAttribute() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> doctree.visitAttribute(mock(AttributeTree.class), new StringBuilder()));
  }

  @Test
  public void visitAuthor() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> doctree.visitAuthor(mock(AuthorTree.class), new StringBuilder()));
  }

  @Test
  public void visitComment() {
    CommentTree commentTree = comment("comment");
    assertThat(doctree.visitComment(commentTree, new StringBuilder()).toString())
        .isEqualTo("<text>comment</text>");
  }

  @Test
  public void visitDeprecated() {
    DeprecatedTree deprecatedTree =
        TreeBuilder.of(DeprecatedTree.class)
            .withContent(
                DeprecatedTree::getBody,
                comments(
                    "because we gotta launch stuff for promo.",
                    "gotta move those protos to production."))
            .build();

    assertThat(doctree.visitDeprecated(deprecatedTree, new StringBuilder()).toString())
        .isEqualTo(
            "<break/><b><text>DEPRECATED</text></b><text> </text><text>because we gotta launch"
                + " stuff for promo.</text><text>gotta move those protos to"
                + " production.</text><break/>");
  }

  @Test
  public void visitDocComment() {
    DocCommentTree docCommentTree =
        TreeBuilder.of(DocCommentTree.class)
            .withContent(DocCommentTree::getBody, comments("body 1", "body 2"))
            .withContent(DocCommentTree::getFullBody, comments("preamble", "body 1", "body 2"))
            .withContent(DocCommentTree::getBlockTags, comments("block 1", "block 2"))
            .build();

    assertThat(doctree.visitDocComment(docCommentTree, new StringBuilder()).toString())
        .isEqualTo(
            "<text>preamble</text><text>body 1</text><text>body 2</text><text>block"
                + " 1</text><text>block 2</text>");
  }

  @Test
  public void visitEndElement() {
    Map<String, String> wants =
        ImmutableMap.of(
            "ol",
            "</numlist>",
            "ul",
            "</list>",
            "pre",
            "</block>",
            "code",
            "</inline>",
            "li",
            "</item>",
            "HELLOWORLD",
            "</HELLOWORLD>");
    wants.forEach(
        (tag, want) -> {
          EndElementTree endElementTree =
              TreeBuilder.of(EndElementTree.class)
                  .withContent(EndElementTree::getName, name(tag))
                  .build();

          assertThat(doctree.visitEndElement(endElementTree, new StringBuilder()).toString())
              .isEqualTo(want);
        });
  }

  @Test
  public void visitEntity() {
    EntityTree entityTree =
        TreeBuilder.of(EntityTree.class)
            .withContent(EntityTree::getName, name("gt"))
            .withContent(EntityTree::toString, "&gt;")
            .build();
    assertThat(doctree.visitEntity(entityTree, new StringBuilder()).toString())
        .isEqualTo("<text>></text>");
  }

  @Test
  public void visitErroneous() {
    ErroneousTree erroneousTree =
        TreeBuilder.of(ErroneousTree.class).withContent(ErroneousTree::getBody, "oops").build();
    assertThat(doctree.visitErroneous(erroneousTree, new StringBuilder()).toString())
        .isEqualTo("<text>oops</text>");
  }

  @Test
  public void visitIdentifier() {
    IdentifierTree identifierTree =
        TreeBuilder.of(IdentifierTree.class)
            .withContent(IdentifierTree::getName, name("bob"))
            .build();
    assertThat(doctree.visitIdentifier(identifierTree, new StringBuilder()).toString())
        .isEqualTo("<text>bob</text>");
  }

  @Test
  public void visitInheritDoc() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> doctree.visitInheritDoc(mock(InheritDocTree.class), new StringBuilder()));
  }

  @Test
  public void visitLink() {
    LinkTree linkTree =
        TreeBuilder.of(LinkTree.class)
            .withContent(
                LinkTree::getReference,
                TreeBuilder.of(ReferenceTree.class)
                    .withContent(ReferenceTree::getSignature, "hello(String world)")
                    .build())
            .build();
    assertThat(doctree.visitLink(linkTree, new StringBuilder()).toString())
        .isEqualTo("<text>hello(String world)</text>");
  }

  @Test
  public void visitLiteral() {
    LiteralTree literalTree =
        TreeBuilder.of(LiteralTree.class)
            .withContent(
                LiteralTree::getBody,
                TreeBuilder.of(TextTree.class)
                    .withContent(TextTree::getBody, "omg, like, literally")
                    .build())
            .build();
    assertThat(doctree.visitLiteral(literalTree, new StringBuilder()).toString())
        .isEqualTo("<inline><text>omg, like, literally</text></inline>");
  }

  @Test
  public void visitParam() {
    ParamTree paramTree =
        TreeBuilder.of(ParamTree.class)
            .withContent(
                ParamTree::getName,
                TreeBuilder.of(IdentifierTree.class)
                    .withContent(IdentifierTree::getName, name("mary"))
                    .build())
            .withContent(ParamTree::getDescription, comments("had a little", "lamb"))
            .build();

    // Params tags need to be handled separately, so we must skip them.
    assertThat(doctree.visitParam(paramTree, new StringBuilder()).toString()).isEmpty();
  }

  @Test
  public void visitReference() {
    ReferenceTree referenceTree =
        TreeBuilder.of(ReferenceTree.class)
            .withContent(ReferenceTree::getSignature, "hello(String world)")
            .build();

    assertThat(doctree.visitReference(referenceTree, new StringBuilder()).toString())
        .isEqualTo("<inline><text>hello(String world)</text></inline>");
  }

  @Test
  public void visitReturn() {
    ReturnTree returnTree =
        TreeBuilder.of(ReturnTree.class)
            .withContent(ReturnTree::getDescription, comments("all sales", "are final"))
            .build();

    // Return tags need to be handled separately, so we must skip them.
    assertThat(doctree.visitReturn(returnTree, new StringBuilder()).toString()).isEmpty();
  }

  @Test
  public void visitSee() {
    SeeTree seeTree =
        TreeBuilder.of(SeeTree.class)
            .withContent(
                SeeTree::getReference, comments("I spy", "with my little i", "not a real number"))
            .build();

    assertThat(doctree.visitSee(seeTree, new StringBuilder()).toString())
        .isEqualTo(
            "<text>(see </text><text>I spy</text><text>with my little i</text><text>not a real"
                + " number</text><text>)</text>");
  }

  @Test
  public void visitSerial() {
    assertThat(doctree.visitSerial(mock(SerialTree.class), new StringBuilder()).toString())
        .isEmpty();
  }

  @Test
  public void visitSerialData() {
    assertThat(doctree.visitSerialData(mock(SerialDataTree.class), new StringBuilder()).toString())
        .isEmpty();
  }

  @Test
  public void visitSerialField() {
    assertThat(
            doctree.visitSerialField(mock(SerialFieldTree.class), new StringBuilder()).toString())
        .isEmpty();
  }

  @Test
  public void visitSince() {
    SinceTree sinceTree =
        TreeBuilder.of(SinceTree.class)
            .withContent(SinceTree::getBody, comments("windows 95"))
            .build();

    assertThat(doctree.visitSince(sinceTree, new StringBuilder()).toString())
        .isEqualTo("<text>(since </text><text>windows 95</text><text>)</text>");
  }

  @Test
  public void visitStartElement() {
    Map<String, String> wants =
        ImmutableMap.of(
            "ol",
            "<numlist>",
            "ul",
            "<list>",
            "pre",
            "<block>",
            "code",
            "<inline>",
            "li",
            "<item>",
            "HELLOWORLD",
            "<HELLOWORLD>");
    wants.forEach(
        (tag, want) -> {
          StartElementTree startElementTree =
              TreeBuilder.of(StartElementTree.class)
                  .withContent(StartElementTree::getName, name(tag))
                  .build();

          assertThat(doctree.visitStartElement(startElementTree, new StringBuilder()).toString())
              .isEqualTo(want);
        });
  }

  @Test
  public void visitText() {
    TextTree textTree =
        TreeBuilder.of(TextTree.class).withContent(TextTree::getBody, "lorem ipsum").build();

    assertThat(doctree.visitText(textTree, new StringBuilder()).toString())
        .isEqualTo("<text>lorem ipsum</text>");
  }

  @Test
  public void visitThrows() {
    ThrowsTree throwsTree =
        TreeBuilder.of(ThrowsTree.class)
            .withContent(
                ThrowsTree::getExceptionName,
                TreeBuilder.of(ReferenceTree.class)
                    .withContent(ReferenceTree::getSignature, "IllegalArgumentException")
                    .build())
            .withContent(
                ThrowsTree::getDescription,
                comments(
                    "while arguing", "the plaintiff avoided taxes", "this was an illegal argument"))
            .build();

    // Throws tags need to be handled separately, so we must skip them.
    assertThat(doctree.visitThrows(throwsTree, new StringBuilder()).toString()).isEmpty();
  }

  @Test
  public void visitUnknownBlockTag() {
    UnknownBlockTagTree unknownBlockTagTree =
        TreeBuilder.of(UnknownBlockTagTree.class)
            .withContent(UnknownBlockTagTree::getTagName, "woop")
            .withContent(UnknownBlockTagTree::getContent, comments("there it is"))
            .build();

    assertThat(doctree.visitUnknownBlockTag(unknownBlockTagTree, new StringBuilder()).toString())
        .isEqualTo("<woop><text>there it is</text></woop>");
  }

  @Test
  public void visitUnknownInlineTag() {
    UnknownInlineTagTree unknownInlineTagTree =
        TreeBuilder.of(UnknownInlineTagTree.class)
            .withContent(UnknownInlineTagTree::getTagName, "woop")
            .withContent(UnknownInlineTagTree::getContent, comments("there it is"))
            .build();

    assertThat(doctree.visitUnknownInlineTag(unknownInlineTagTree, new StringBuilder()).toString())
        .isEqualTo("<woop><text>there it is</text></woop>");
  }

  @Test
  public void visitValue() {
    ValueTree valueTree =
        TreeBuilder.of(ValueTree.class)
            .withContent(
                ValueTree::getReference,
                TreeBuilder.of(ReferenceTree.class)
                    .withContent(ReferenceTree::getSignature, "priceless")
                    .build())
            .build();

    assertThat(doctree.visitValue(valueTree, new StringBuilder()).toString())
        .isEqualTo("<text>priceless</text>");
  }

  @Test
  public void visitVersion() {
    VersionTree versionTree =
        TreeBuilder.of(VersionTree.class)
            .withContent(VersionTree::getBody, comments("v1", ".2"))
            .build();

    assertThat(doctree.visitVersion(versionTree, new StringBuilder()).toString())
        .isEqualTo("<text>version: </text><text>v1</text><text>.2</text>");
  }

  @Test
  public void visitOther() {
    // We do not support any other nodes.
    assertThat(doctree.visitOther(mock(DocTree.class), new StringBuilder()).toString()).isEmpty();
  }

  private CommentTree comment(String text) {
    return TreeBuilder.of(CommentTree.class)
        .withContent(CommentTree::getBody, text)
        .visitedVia(doctree::visitComment)
        .build();
  }

  private List<CommentTree> comments(String... text) {
    return Arrays.stream(text).map(this::comment).collect(Collectors.toList());
  }

  private static class TreeBuilder<T extends DocTree> {
    private final T tree;

    private TreeBuilder(T tree) {
      this.tree = tree;
    }

    public static <T extends DocTree> TreeBuilder<T> of(Class<T> clazz) {
      return new TreeBuilder<>(mock(clazz));
    }

    public <U> TreeBuilder<T> withContent(Function<T, U> contentFn, U content) {
      when(contentFn.apply(tree)).thenReturn(content);
      return this;
    }

    public TreeBuilder<T> visitedVia(BiFunction<T, StringBuilder, StringBuilder> visitor) {
      when(tree.accept(any(), any())).then(i -> visitor.apply(tree, i.getArgument(1)));
      return this;
    }

    public T build() {
      return tree;
    }
  }

  private static Name name(String name) {
    Name mockName = mock(Name.class);
    when(mockName.toString()).thenReturn(name);
    return mockName;
  }
}
