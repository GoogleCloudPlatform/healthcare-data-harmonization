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

import com.google.common.collect.ImmutableList;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.TextTree;
import java.util.List;

/**
 * Class which acts as a placeholder to assist in identifying plugin targets with missing doc
 * strings for their construct methods.
 */
public class ToDoDocCommentTree implements DocCommentTree {
  private static final String placeHolderText =
      "TODO: This is missing documentation. It will be added soon.";
  private static final ToDoText docText = new ToDoText(placeHolderText);

  @Override
  public List<? extends DocTree> getFirstSentence() {
    return ImmutableList.of();
  }

  @Override
  public List<? extends DocTree> getBody() {
    return ImmutableList.of(docText);
  }

  @Override
  public List<? extends DocTree> getFullBody() {
    return ImmutableList.of(docText);
  }

  @Override
  public List<? extends DocTree> getBlockTags() {
    return ImmutableList.of();
  }

  @Override
  public Kind getKind() {
    return Kind.DOC_COMMENT;
  }

  @Override
  public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
    return visitor.visitDocComment(this, data);
  }

  private static class ToDoText implements TextTree {
    public final String text;

    ToDoText(String text) {
      this.text = text;
    }

    @Override
    public Kind getKind() {
      return Kind.TEXT;
    }

    @Override
    public <R, D> R accept(DocTreeVisitor<R, D> v, D d) {
      return v.visitText(this, d);
    }

    @Override
    public String getBody() {
      return text;
    }
  }
}
