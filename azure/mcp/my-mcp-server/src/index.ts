import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod"; // Import zod for schema validation

async function main() {
  // 1. Create an MCP server instance
  const server = new McpServer(
    {
      // Server identification
      name: "GreetingServer",
      version: "1.0.1",
    },
    {
      // Declare server capabilities
      capabilities: {
        tools: { listChanged: false }, // We support tools, no dynamic changes
        // resources: {}, // Uncomment if supporting resources
        // prompts: {},   // Uncomment if supporting prompts
      },
    }
  );

  // 2. Define the input schema for the 'greet' tool using zod
  const greetInputSchema = z.object({
    name: z.string().min(1).describe("The name of the person to greet"),
  });

  // 3. Add the 'greet' tool implementation
  server.tool(
    "greet", // Tool name
    greetInputSchema.shape, // Convert schema to raw shape for validation
    async (input) => {
      // Input is automatically validated against the schema
      const message = `Hello, ${input?.name}! Welcome to MCP.`; // Use optional chaining for safety
      console.error(`Tool 'greet' called with name: ${input?.name}`); // Use optional chaining for safety

      // Return the result conforming to CallToolResultContent
      return {
        content: [{ type: "text", text: message }],
        // isError: false, // Default is false
      };
    }
  );

  // 4. Create a transport (stdio for this example)
  const transport = new StdioServerTransport();

  // 5. Connect the server to the transport and start listening
  try {
    await server.connect(transport);
    console.error("Greeting MCP Server is running and connected via stdio."); // Log to stderr
  } catch (error) {
    console.error("Failed to connect server:", error);
    process.exit(1);
  }

  // Keep the server running (for stdio, it runs until stdin closes)
  // For other transports like HTTP, you'd typically have a server.listen() call
}

// Run the main function
main().catch((error) => {
  console.error("Unhandled error during server startup:", error);
  process.exit(1);
});