# Azure Vector MCP Server

## How to Run the MCP Server

### Prerequisites
1. Ensure Node.js is installed on your system.
2. Install dependencies by running:
   ```bash
   npm install
   ```

### Environment Variables
Set the following environment variables:
- `OPENAI_API_KEY`: Your Azure OpenAI Service API key.
- `AZURE_SEARCH_API_KEY`: Your Azure Cognitive Search API key.
- `AZURE_SEARCH_SERVICE_NAME`: The name of your Azure Cognitive Search service.
- `AZURE_SEARCH_INDEX_NAME`: The name of your Azure Cognitive Search index.

You can set these variables in a `.env` file or directly in your shell.

Example `.env` file:
```env
OPENAI_API_KEY=your-openai-api-key
AZURE_SEARCH_API_KEY=your-azure-search-api-key
AZURE_SEARCH_SERVICE_NAME=your-search-service-name
AZURE_SEARCH_INDEX_NAME=your-index-name
```

### Running the Server
1. Build the server:
   ```bash
   npm run build
   ```
2. Start the server:
   ```bash
   node build/index.js
   ```

### Testing the Server
Once the server is running, you can interact with it using the tools:
- `generate_embeddings`: Generate embeddings for a document.
- `index_embeddings`: Index embeddings into Azure Cognitive Search.
- `query_embeddings`: Query embeddings for document retrieval.

### Notes
- Ensure all environment variables are correctly set before running the server.
- Use the MCP client to send requests to the server.
