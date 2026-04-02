package com.antlers.support.statamic

data class TagParameter(
    val name: String,
    val description: String,
    val required: Boolean = false
)

/**
 * Hand-maintained parameter metadata for common Statamic tags.
 * Verified against Statamic 4.x documentation.
 */
object StatamicTagParameters {
    private val params: Map<String, List<TagParameter>> = mapOf(
        "collection" to listOf(
            TagParameter("from", "Collection handle(s); pipe-separate multiple or use * for all", required = true),
            TagParameter("not_from", "Collections to exclude when using *"),
            TagParameter("show_future", "Include date-based entries from the future (default: false)"),
            TagParameter("show_past", "Include past date-based entries (default: true)"),
            TagParameter("since", "Earliest date for entries; accepts plain English or date variables"),
            TagParameter("until", "Latest date for entries"),
            TagParameter("sort", "Sort by field:direction; pipe-separate multiple or use random"),
            TagParameter("limit", "Maximum number of entries returned"),
            TagParameter("offset", "Number of entries to skip"),
            TagParameter("filter", "Custom query scope handle (snake_case)"),
            TagParameter("query_scope", "Custom query scope handle (alias for filter)"),
            TagParameter("taxonomy", "Filter by taxonomy terms"),
            TagParameter("paginate", "Enable pagination; true or per-page count"),
            TagParameter("page_name", "Query string variable for page number (default: page)"),
            TagParameter("on_each_side", "Pagination links on each side of current page (default: 3)"),
            TagParameter("as", "Alias results into a new variable loop"),
            TagParameter("scope", "Prefix variables with scope identifier"),
            TagParameter("locale", "Display content in selected locale"),
            TagParameter("site", "Display content from selected site"),
            TagParameter("redirects", "Include entries with redirects (default: false)"),
        ),
        "nav" to listOf(
            TagParameter("from", "Navigation handle"),
            TagParameter("max_depth", "Maximum depth of nesting"),
            TagParameter("include_home", "Include the home page"),
            TagParameter("sort", "Sort field and direction"),
            TagParameter("scope", "Variable scope prefix"),
            TagParameter("select", "Fields to select"),
        ),
        "taxonomy" to listOf(
            TagParameter("from", "Taxonomy handle", required = true),
            TagParameter("sort", "Sort field and direction"),
            TagParameter("limit", "Maximum number of terms"),
            TagParameter("offset", "Number of terms to skip"),
            TagParameter("min_count", "Minimum entries count to include term"),
            TagParameter("scope", "Variable scope prefix"),
            TagParameter("as", "Alias for the loop variable"),
        ),
        "form" to listOf(
            TagParameter("in", "Form handle", required = true),
            TagParameter("redirect", "URL to redirect after submission"),
            TagParameter("error_redirect", "URL to redirect on error"),
            TagParameter("allow_request_redirect", "Allow redirect from request"),
            TagParameter("attr", "HTML attributes for the form tag"),
            TagParameter("files", "Enable file uploads"),
        ),
        "cache" to listOf(
            TagParameter("for", "Cache duration (e.g. 60 minutes, 1 day)"),
            TagParameter("key", "Unique cache key"),
            TagParameter("scope", "Cache scope (site, page, user)"),
            TagParameter("tags", "Cache tags for invalidation"),
        ),
        "glide" to listOf(
            TagParameter("src", "Image source path or URL", required = true),
            TagParameter("width", "Output width in pixels"),
            TagParameter("height", "Output height in pixels"),
            TagParameter("fit", "Fit mode (contain, max, fill, stretch, crop)"),
            TagParameter("crop", "Crop position (e.g. center, top-left)"),
            TagParameter("quality", "Image quality (0-100)"),
            TagParameter("format", "Output format (jpg, png, webp)"),
            TagParameter("blur", "Blur amount (0-100)"),
            TagParameter("bg", "Background color for transparent images"),
        ),
        "search" to listOf(
            TagParameter("index", "Search index to use"),
            TagParameter("query", "Search query string"),
            TagParameter("limit", "Maximum results"),
            TagParameter("offset", "Number of results to skip"),
            TagParameter("as", "Alias for the results variable"),
            TagParameter("supplement_data", "Fetch full entries from search results"),
        ),
        "assets" to listOf(
            TagParameter("container", "Asset container handle"),
            TagParameter("folder", "Folder path within container"),
            TagParameter("limit", "Maximum number of assets"),
            TagParameter("sort", "Sort field and direction"),
            TagParameter("as", "Alias for the loop variable"),
        ),
        "session" to listOf(
            TagParameter("as", "Variable scope for output"),
        ),
        "redirect" to listOf(
            TagParameter("to", "URL to redirect to", required = true),
            TagParameter("response", "HTTP response code (301, 302)"),
        ),
        "link" to listOf(
            TagParameter("to", "Entry ID or URL to link to", required = true),
            TagParameter("absolute", "Generate absolute URL"),
        ),
        "svg" to listOf(
            TagParameter("src", "Path to SVG file", required = true),
            TagParameter("class", "CSS class to add to SVG element"),
            TagParameter("sanitize", "Sanitize SVG content"),
        ),
        "partial" to listOf(
            TagParameter("src", "Partial view path"),
            TagParameter("when", "Condition to render the partial"),
            TagParameter("unless", "Negated condition to render the partial"),
        ),
        "loop" to listOf(
            TagParameter("times", "Number of iterations", required = true),
            TagParameter("as", "Alias for the current iteration index"),
        ),
    )

    fun forTag(name: String): List<TagParameter> {
        val rootName = name.substringBefore(':')
        return params[rootName].orEmpty()
    }

    fun hasParameters(name: String): Boolean {
        val rootName = name.substringBefore(':')
        return params.containsKey(rootName)
    }

    fun allEntries(): Map<String, List<TagParameter>> = params
}
