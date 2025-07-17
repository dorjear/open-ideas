# Guide to Obtaining Your Azure Search API Key

## Step 1: Log in to Azure Portal
1. Go to [Azure Portal](https://portal.azure.com).
2. Log in with your Azure account credentials.

## Step 2: Navigate to Your Search Service
1. In the Azure Portal, search for "Cognitive Search" in the search bar.
2. Select your search service (`servicelu`).

## Step 3: Access Keys
1. In the left-hand menu, click on "Keys".
2. You will see two keys: `Primary Key` and `Secondary Key`.
3. Copy the `Primary Key` to use as your `AZURE_SEARCH_API_KEY`.

## Notes
- Keep your API key secure and do not share it publicly.
- You can regenerate keys if needed, but this will invalidate the old keys.