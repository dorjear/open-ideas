# Usage Guide for Embedding Documents

## Step 1: Start the MCP Server
Run the server using the following command:
```bash
node build/index.js
```

## Step 2: Generate Embeddings for Your Document
Use the `generate_embeddings` tool to create embeddings for your document. Send a request to the MCP server with the document content.

### Example Request
```json
{
  "tool": "generate_embeddings",
  "parameters": {
    "input": "Your document content here"
  }
}
```

### Example Response
```json
{
  "content": [
    {
      "type": "text",
      "text": "[0.1, 0.2, 0.3, ...]"
    }
  ]
}
```

## Step 3: Index the Document and Embeddings
Use the `index_embeddings` tool to store the document and its embeddings in Azure Cognitive Search.

### Example Request
```json
{
  "tool": "index_embeddings",
  "parameters": {
    "id": "document-id",
    "content": "Your document content here",
    "embedding": [0.1, 0.2, 0.3, ...]
  }
}
```

### Example Response
```json
{
  "content": [
    {
      "type": "text",
      "text": "Indexing successful: { ... }"
    }
  ]
}
```

## Step 4: Query Documents
Use the `query_embeddings` tool to retrieve documents based on semantic similarity.

### Example Request
```json
{
  "tool": "query_embeddings",
  "parameters": {
    "queryVector": [0.1, 0.2, 0.3, ...]
  }
}
```

### Example Response
```json
{
  "content": [
    {
      "type": "text",
      "text": "Query results: { ... }"
    }
  ]
}
```

## Notes
- Ensure the server is running before sending requests.
- Use an MCP client to interact with the server.
- Replace placeholders with actual document content and embeddings.