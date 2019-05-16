/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   May 16, 2019 (awalter): created
 */
package org.knime.workbench.explorer.dbworkspace;

import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.workflowalizer.VersionSerializer;
import org.knime.core.util.workflowalizer2.ConfigBaseSerializer;
import org.knime.core.util.workflowalizer2.Node;
import org.knime.core.util.workflowalizer2.Workflow;
import org.knime.core.util.workflowalizer2.WorkflowBundle;
import org.knime.core.util.workflowalizer2.Workflowalizer2;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

/**
 *
 * @author awalter
 * @since 8.4
 */
public class MongoImport {

    public static void uploadWorkflow(final Path path, final String workflowName, final MongoClient client) {
        WorkflowBundle pojo;
        try {
            pojo = Workflowalizer2.readWorkflowBundle(path, workflowName);
        } catch (IOException e) {
            System.out.println("Cannot access file");
            e.printStackTrace();
            return;
        } catch (InvalidSettingsException e) {
            System.out.println("Invalid settings");
            e.printStackTrace();
            return;
        }

        uploadWorkflow(pojo.getWorkflow(), pojo.getNodes(), client);
    }

    public static void uploadWorkflow(final Workflow workflow, final List<Node> nodes, final MongoClient client) {
        try {
            final MongoDatabase database = client.getDatabase("knime");
            final MongoCollection<Document> wkf = database.getCollection("workflows");
            final MongoCollection<Document> nd = database.getCollection("nodes");

            final ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(Include.NON_ABSENT);
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.registerModule(new Jdk8Module());
            final SimpleModule sm = new SimpleModule();
            sm.addSerializer(new VersionSerializer());
            sm.addSerializer(new ConfigBaseSerializer());
            mapper.registerModule(sm);
            final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
            mapper.setDateFormat(df);
            final String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(workflow);

            final Document d = Document.parse(json);
            wkf.insertOne(d);

            final List<Document> nodeDocs = new ArrayList<>(nodes.size());
            for (final Node node : nodes) {
                final String j = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
                final Document n = Document.parse(j);
                nodeDocs.add(n);
            }
            nd.insertMany(nodeDocs);
        } catch (final JsonProcessingException e) {
            System.out.println("Failed to write JSON");
            System.out.println(e);
            e.printStackTrace();
            return;
        }
    }

    public static WorkflowBundle exportWorkflow(final String wkfId, final String connection) {
        try (final MongoClient client = MongoClients.create(connection)) {
            return exportWorkflow(wkfId, client);
        }
    }

    public static WorkflowBundle exportWorkflow(final String wkfId, final MongoClient client) {
        final MongoDatabase database = client.getDatabase("knime");
        final MongoCollection<Document> wkf = database.getCollection("workflows");
        final MongoCollection<Document> nd = database.getCollection("nodes");

        final Document wkfDoc = wkf.find(Filters.eq("id", wkfId)).first();
        final List<Document> ndDocs = new ArrayList<>();
        nd.find(Filters.regex("id", wkfId + "#node_[1-9]+")).into(ndDocs);

        final Workflow workflow = Workflowalizer2.convert(wkfDoc, Workflow.class);
        final List<Node> nodes = new ArrayList<>(ndDocs.size());
        for (final Document d : ndDocs) {
            nodes.add(Workflowalizer2.convert(d, Node.class));
        }

        return new WorkflowBundle() {

            @Override
            public Workflow getWorkflow() {
                return workflow;
            }

            @Override
            public List<Node> getNodes() {
                return nodes;
            }
        };
    }



    public static List<Workflow> exportAllWorkflows(final String connection) {
        try (final MongoClient client = MongoClients.create(connection)) {
            return exportAllWorkflows(client);
         }
    }

    public static List<Workflow> exportAllWorkflows(final MongoClient client) {
        final MongoDatabase database = client.getDatabase("knime");
        final MongoCollection<Document> wkf = database.getCollection("workflows");
        final List<Document> docs = new ArrayList<>();
        wkf.find().into(docs);
        return docs.stream().map(d -> {
            return Workflowalizer2.convert(d, Workflow.class);
        }).collect(Collectors.toList());
    }

}
