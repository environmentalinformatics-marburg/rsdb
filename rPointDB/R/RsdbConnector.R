RsdbConnector_public <- list( #      *********** public *********************************
  
  initialize = function(base_url, username=NULL, password=NULL, ssl_verifypeer=TRUE) {
    private$base_url <- base_url
    private$username <- username
    private$password <- password
    private$ssl_verifypeer <- ssl_verifypeer
    private$session <- "none"
    private$authentication <- "none"
    self$GET("/pointdb/")
  },
  
  GET = function(url_path, url_query = NULL) {
    r <- self$VERB("GET", url_path, url_query)
    return(r)
  },
  
  POST_json = function(url_path, url_query = NULL, data = NULL) {
    json_data <- charToRaw(jsonlite::toJSON(data, auto_unbox=TRUE))
    r <- self$VERB("POST", url_path, url_query, json_data)
    return(r)
  },
  
  POST_raw = function(url_path, url_query = NULL, data = NULL) {
    r <- self$VERB("POST", url_path, url_query, data)
    return(r)
  },
  
  VERB = function(verb, url_path, url_query = NULL, data = NULL) {
    url <- paste0(private$base_url, url_path)
    httr::handle_reset(url)
    if(private$authentication == "none") {
      r <- httr::VERB(verb, url, query = url_query, body = data, httr::config(ssl_verifypeer = private$ssl_verifypeer))
    } else if(private$authentication == "login_sha2_512") {
      r <- httr::VERB(verb, url, query = url_query, body = data, httr::config(ssl_verifypeer = private$ssl_verifypeer), httr::set_cookies(session=private$session))
    } else if(private$authentication == "Digest") {
      r <- httr::VERB(verb, url, query = url_query, body = data, httr::config(ssl_verifypeer = private$ssl_verifypeer), httr::authenticate(private$username, private$password, "digest"))
    } else if(private$authentication == "basic") {
      r <- httr::VERB(verb, url, query = url_query, body = data, httr::config(ssl_verifypeer = private$ssl_verifypeer), httr::authenticate(private$username, private$password, "basic"))
    }else {
      stop("unkown authentication method")
    }
    if(r$status_code == 401) {
      auth <- r$headers$`www-authenticate`
      if(startsWith(auth, "login_sha2_512")) {
        server_nonce <- getParam(auth, "server_nonce")
        client_nonce <- "OvHcQiqR"
        user_hash_size <- as.integer(getParam(auth, "user_hash_size"))
        user_salt <- getParam(auth, "user_salt")
        salt <- getParam(auth, "salt")
        
        h_user <- trans(paste0(user_salt, private$username, user_salt)) 
        h_user <- substring(h_user, nchar(h_user) + 1 - user_hash_size)
        
        h_inner <- trans(paste0(salt, private$username, salt, private$password, salt))
        text <- paste0(server_nonce, client_nonce, h_inner, client_nonce, server_nonce)
        h <- trans(text)
        url_login <- paste0(private$base_url,"/login?user=",h_user,"&server_nonce=", server_nonce, "&client_nonce=", client_nonce, "&hash=", h)
        httr::handle_reset(url)
        r <- httr::GET(url_login, httr::config(ssl_verifypeer = private$ssl_verifypeer))
        cc <- httr::cookies(r)
        private$session <- cc[cc$name == 'session']$value
        httr::handle_reset(url)
        r <- httr::VERB(verb, url, query = url_query, body = data, httr::config(ssl_verifypeer = private$ssl_verifypeer), httr::set_cookies(session=private$session))
        private$authentication <- "login_sha2_512"
      } else if(startsWith(auth, "Digest")) {
        r <- httr::VERB(verb, url, query = url_query, body = data, httr::config(ssl_verifypeer = private$ssl_verifypeer), httr::authenticate(private$username, private$password, "digest"))
        private$authentication <- "Digest"
      } else if(startsWith(auth, "basic")) {
        r <- httr::VERB(verb, url, query = url_query, body = data, httr::config(ssl_verifypeer = private$ssl_verifypeer), httr::authenticate(private$username, private$password, "basic"))
        private$authentication <- "basic"
      } else {
        stop("unkown authentication method")
      }
    }

    if(r$status_code == 401) {
      stop("no connection to RSDB: wrong username / password ?")
    }
    if(r$status_code == 404) {
      text <- httr::content(r, as = "text", encoding = "UTF-8")
      stop("Error in RSDB: ", text)
    }
    if(r$status_code != 200) {
      text <- httr::content(r, as = "text", encoding = "UTF-8")
      stop("RSDB - Error: ", r$status_code, ": ", text)
    }
    
    t <- httr::http_type(r)
    if(t == "application/json") {
      text <- httr::content(r, as = "text", encoding = "UTF-8")
      json <- jsonlite::fromJSON(text)
      return(json)
    }
    if(t == "text/plain") {
      text <- httr::content(r, as = "text", encoding = "UTF-8")
      return(text)
    }
    if(t == "application/octet-stream") {
      data <- httr::content(r, as = "raw")
      if(data[1] == 0x52 & data[2] == 0x44 & data[3] == 0x41 & data[4] == 0x54) { # RDAT
        con <- rawConnection(data)
        rdat <- read_RDAT(con)
        close(con)
        return(rdat)
      } else {
        warning("unknown binary content")
        return(data)
      }
    }
    warning("unknown content type")
    return(r)
  }

  
)

RsdbConnector_active <- list( #      *********** active *********************************

)

RsdbConnector_private <- list( #      *********** private *********************************
  
  base_url = NULL,
  username = NULL,
  password = NULL,
  ssl_verifypeer = TRUE,
  session = "none",
  authentication = "none"
)


#' @export
RsdbConnector <- R6::R6Class("RsdbConnector",
                             public = RsdbConnector_public,
                             active = RsdbConnector_active,
                             private = RsdbConnector_private,
                             lock_class = TRUE,
                             lock_objects = TRUE,
                             portable = FALSE,
                             class = TRUE,
                             cloneable = FALSE
)


getParam <- function(text, key) {
  # pattern: .......KEY="VVVVVV"........
  p <- paste0('^(.*)', key, '="([^"]*)"(.*)')
  v <- gsub(p, '\\2', text)
  if(text == v) {
    return(NULL)
  }
  return(v)
}

trans <- function(text) {
  h <- openssl::sha512(text)
  return(h)
}

