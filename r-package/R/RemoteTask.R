RemoteTask_public <- list( #      *********** public *********************************

  initialize = function(rsdbConnector, id) {
    private$rsdbConnector <- rsdbConnector
    private$id_ <- id
  },

  wait = function(polling_interval = 0.5) {
    busy <- '-'
    status <- NULL

    while(TRUE) {

      wenv <- new.env()
      wenv$status <- status
      tryCatch({
        wenv$status <- self$status
      }, error = function(err) {
        if(startsWith(err$message, 'Timeout was reached')) {
          cat(err$message, '  try again...\n')
          wenv$status <- NULL
        } else {
          stop(err)
        }
      })
      status <- wenv$status

      if(!is.null(status)) {
        if(status$active) {
          cat("\r", busy, status$status, ' ', (status$runtime / 1000), 's', '-', status$message, '                                             ')
        } else {
          break
        }
      }
      Sys.sleep(polling_interval)

      if(busy == '-') {
        busy = '\\'
      } else if(busy == '\\') {
        busy = '|'
      } else if(busy == '|') {
        busy = '/'
      } else if(busy == '/') {
        busy = '-'
      }
    }

    cat("\r", status$status, ' ', (status$runtime / 1000), 's', '-', status$message, '                                             ')
    return(status)
  }

) #***********************************************************************************

RemoteTask_active <- list( #      *********** active *********************************

  id = function() {
    return(private$id_)
  },
  status = function() {
    response <- private$rsdbConnector$GET(paste0("/api/remote_tasks/", private$id_), NULL, 60)
    return(response$remote_task)
  }

) #***********************************************************************************

RemoteTask_private <- list( #      *********** private *******************************

  rsdbConnector = NULL,
  id_ = NULL

) #***********************************************************************************



#' RemoteTask class
#'
#' RemoteTask class provides methods to manage a remote task running at RSDB server.
#'
#' @keywords remote task
#' @seealso \link{RemoteSensing}
#' @author woellauer
#'
#' @usage
#' # Connect to remote sensing database.
#' remotesensing <- RemoteSensing$new(url, userpwd=NULL)
#'
#' # Run a remote task at RSDB server.
#' remote_task <- remotesensing$submit_task(task)
#'
#' # Get current task status information.
#' status <- remote_task$status
#'
#' # Wait until task has finished processing, check task status every polling_interval seconds.
#' status <- remote_task$wait(polling_interval = 0.5)
#'
#' @format
#' RemoteTask \link{R6Class} object.
#'
#'
#' @examples
#'
#' remotesensing <- RSDB::RemoteSensing$new('http://localhost:8080', userpwd='user:password')
#'
#' task <- list(task_rasterdb = 'count_pixels', rasterdb = 'raster1')
#'
#' remote_task <- remotesensing$submit_task(task)
#'
#' status <- remote_task$wait()
#'
#' status$message
#'
#' @export
RemoteTask <- R6::R6Class("RemoteTask",
                          public = RemoteTask_public,
                          active = RemoteTask_active,
                          private = RemoteTask_private,
                          lock_class = TRUE,
                          lock_objects = TRUE,
                          portable = FALSE,
                          class = TRUE,
                          cloneable = FALSE
)
