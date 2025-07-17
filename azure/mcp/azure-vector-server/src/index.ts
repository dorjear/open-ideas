#!/usr/bin/env node
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import axios from "axios";

const OPENAI_API_KEY = process.env.OPENAI_API_KEY;
const AZURE_SEARCH_API_KEY = process.env.AZURE_SEARCH_API_KEY;
const AZURE_SEARCH_SERVICE_NAME = process.env.AZURE_SEARCH_SERVICE_NAME;
const AZURE_SEARCH_INDEX_NAME = process.env.AZURE_SEARCH_INDEX_NAME;

if (!OPENAI_API_KEY || !AZURE_SEARCH_API_KEY || !AZURE_SEARCH_SERVICE_NAME || !AZURE_SEARCH_INDEX_NAME) {
  throw new Error("Environment variables OPENAI_API_KEY, AZURE_SEARCH_API_KEY, AZURE_SEARCH_SERVICE_NAME, and AZURE_SEARCH_INDEX_NAME are required.");
}

// Create an MCP server
const server = new McpServer({
  name: "azure-vector-server",
  version: "1.0.0",
});

// Axios instance for Azure OpenAI Service
const openAiApi = axios.create({
  baseURL: `https://<region>.api.cognitive.microsoft.com/openai/deployments/<deployment-id>/embeddings`,
  headers: {
    "Content-Type": "application/json",
    Authorization: `Bearer ${OPENAI_API_KEY}`,
  },
});

// Axios instance for Azure Cognitive Search
const azureSearchApi = axios.create({
  baseURL: `https://${AZURE_SEARCH_SERVICE_NAME}.search.windows.net/indexes/${AZURE_SEARCH_INDEX_NAME}/docs`,
  headers: {
    "Content-Type": "application/json",
    "api-key": AZURE_SEARCH_API_KEY,
  },
});

// Tool for generating embeddings
server.tool(
  "generate_embeddings",
  {
    input: z.string().describe("Document content to generate embeddings for"),
  },
  async ({ input }) => {
    try {
      const response = await openAiApi.post("", { input });
      return {
        content: [
          {
            type: "text",
            text: JSON.stringify(response.data.data[0].embedding, null, 2),
          },
        ],
      };
    } catch (error) {
      return {
        content: [
          {
            type: "text",
            text: `Error generating embeddings: ${error instanceof Error ? error.message : String(error)}`,
          },
        ],
        isError: true,
      };
    }
  }
);

// Tool for indexing embeddings
server.tool(
  "index_embeddings",
  {
    id: z.string().describe("Document ID"),
    content: z.string().describe("Document content"),
    embedding: z.array(z.number()).describe("Embedding vector"),
  },
  async ({ id, content, embedding }) => {
    try {
      const response = await azureSearchApi.post("/index", {
        value: [
          {
            "@search.action": "upload",
            id,
            content,
            embedding,
          },
        ],
      });
      return {
        content: [
          {
            type: "text",
            text: `Indexing successful: ${JSON.stringify(response.data, null, 2)}`,
          },
        ],
      };
    } catch (error) {
      return {
        content: [
          {
            type: "text",
            text: `Error indexing embeddings: ${error instanceof Error ? error.message : String(error)}`,
          },
        ],
        isError: true,
      };
    }
  }
);

// Tool for querying embeddings
server.tool(
  "query_embeddings",
  {
    queryVector: z.array(z.number()).describe("Query vector"),
  },
  async ({ queryVector }) => {
    try {
      const response = await azureSearchApi.post("/search", {
        search: queryVector,
        queryType: "vector",
        fields: ["embedding"],
      });
      return {
        content: [
          {
            type: "text",
            text: `Query results: ${JSON.stringify(response.data.value, null, 2)}`,
          },
        ],
      };
    } catch (error) {
      return {
        content: [
          {
            type: "text",
            text: `Error querying embeddings: ${error instanceof Error ? error.message : String(error)}`,
          },
        ],
        isError: true,
      };
    }
  }
);

// Start receiving messages on stdin and sending messages on stdout
const transport = new StdioServerTransport();
await server.connect(transport);
console.error("Azure Vector MCP server running on stdio");
