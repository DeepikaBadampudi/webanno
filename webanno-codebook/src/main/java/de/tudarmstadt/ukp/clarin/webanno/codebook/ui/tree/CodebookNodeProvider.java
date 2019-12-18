/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.tree;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;

public class CodebookNodeProvider
    implements ITreeProvider<CodebookNode>
{
    private static final long serialVersionUID = -2678498652265164741L;

    private Map<String, CodebookNode> nameToNodes;
    private Map<String, Codebook> nameToCodebooks;
    private Set<CodebookNode> roots;

    /**
     * Construct.
     */
    public CodebookNodeProvider(List<Codebook> allCodebooks)
    {
        this.nameToCodebooks = allCodebooks.parallelStream()
                .collect(Collectors.toMap(Codebook::getName, o -> o));

        this.nameToNodes = allCodebooks.parallelStream().map(CodebookNode::new)
                .collect(Collectors.toMap(CodebookNode::getName, o -> o));

        buildTreeStructures();

        this.roots = this.nameToNodes.values().parallelStream().filter(CodebookNode::isRoot)
                .collect(Collectors.toSet());
    }

    private void setParent(CodebookNode node)
    {
        Codebook book = this.nameToCodebooks.get(node.getName());
        if (book.getParent() == null)
            return;
        node.setParent(this.nameToNodes.get(book.getParent().getName()));
    }

    private void buildTreeStructures()
    {
        for (CodebookNode node : this.nameToNodes.values()) {
            this.setParent(node);
            if (node.getParent() != null)
                this.addChildrenRecursively(node.getParent(), node);
        }
    }

    private void addChildrenRecursively(CodebookNode parent, CodebookNode child)
    {
        parent.addChild(child);
        if (!parent.isRoot())
            this.addChildrenRecursively(parent.getParent(), parent);
    }

    /**
     * Nothing to do.
     */
    @Override
    public void detach()
    {
    }

    @Override
    public Iterator<CodebookNode> getRoots()
    {
        return this.roots.iterator();
    }

    @Override
    public boolean hasChildren(CodebookNode node)
    {
        return !node.isLeaf();
    }

    @Override
    public Iterator<CodebookNode> getChildren(final CodebookNode node)
    {
        return node.getChildren().iterator();
    }

    /**
     * Creates a {@link CodebookNodeModel}.
     */
    @Override
    public IModel<CodebookNode> model(CodebookNode node)
    {
        return new CodebookNodeModel(node);
    }

    /**
     * Get a {@link CodebookNodeModel} by its id.
     */
    public CodebookNode getCodebookNode(String id)
    {
        return findCodebookNode(roots, id);
    }

    private static CodebookNode findCodebookNode(Iterable<CodebookNode> nodes, String id)
    {
        for (CodebookNode node : nodes) {
            if (node.getId().equals(id)) {
                return node;
            }

            CodebookNode temp = findCodebookNode(node.getChildren(), id);
            if (temp != null) {
                return temp;
            }
        }

        return null;
    }

    /*
     * Mapping functions between CodebookNodes and Codebooks and vice versa
     */
    public Codebook getCodebook(final CodebookNode node)
    {
        return nameToCodebooks.get(node.getName());
    }

    public CodebookNode getCodebookNode(final Codebook book)
    {
        return nameToNodes.get(book.getName());
    }

    public Set<Codebook> getCodebooks(final Set<CodebookNode> nodes)
    {
        Set<Codebook> books = new HashSet<>();
        for (CodebookNode node : nodes)
            books.add(this.getCodebook(node));
        return books;
    }

    public Set<CodebookNode> getCodebookNodes(final Set<Codebook> books)
    {
        Set<CodebookNode> nodes = new HashSet<>();
        for (Codebook book : books)
            nodes.add(this.getCodebookNode(book));
        return nodes;
    }

    public Set<Codebook> getChildren(final Codebook book)
    {
        return this.getCodebooks(this.getCodebookNode(book).getChildren());
    }

    public Set<CodebookNode> getPrecedents(final CodebookNode node)
    {
        Set<CodebookNode> parents = new HashSet<>();
        CodebookNode parent = node.getParent();
        while (parent != null) {
            parents.add(parent);
            parent = parent.getParent();
        }
        return parents;
    }

    public Set<CodebookNode> getDescendants(final CodebookNode node,
                                             Set<CodebookNode> allChildren) {
        if(allChildren == null)
            allChildren = new HashSet<>();

        for (CodebookNode child : node.getChildren()) {
            allChildren.add(child);
            getDescendants(child, allChildren);
        }

        return allChildren;
    }

    public Set<Codebook> getPossibleParents(final Codebook book)
    {
        if (book == null || book.getId() == null)
            return new HashSet<>(this.nameToCodebooks.values());

        // all but own children
        Set<Codebook> possibleParents = new HashSet<>(this.nameToCodebooks.values());
        possibleParents.removeAll(this.getChildren(book));
        return possibleParents;
    }
}
