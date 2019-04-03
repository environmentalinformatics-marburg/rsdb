#' Tests if a data.frame of point-data contains at least one point.
#'
#' @param df data.frame of points
#' @return TRUE or FALSE
#' @author woellauer
#' @export
contains_data <- function(df) {
  return(nrow(df)>0)
}

#' Tries to remove erroneous points.
#'
#' The lowest and highest z-value points will be removed (1\% each). So most outliers are not contained in the resulting data.frame.
#'
#' @param df data.frame of points
#' @return cleaned data.frame
#' @author woellauer
#' @export
clean_data <- function(df) {
  qmin <- quantile(df$z, 0.01)
  qmax <- quantile(df$z, 0.99)
  df <- df[qmin<=df$z,]
  df <- df[df$z<=qmax,]
  return(df)
}

#' Calculates some statistical data of a set of points.
#' @param df data.frame of points
#' @return named list statistical values
#' @author woellauer
#' @export
statistics_data <- function(df) {
  r <- list(z_min=min(df$z),
            z_max=max(df$z),
            z_sd=sd(df$z),
            angle_min=min(abs(df$scanAngleRank)),
            angle_max=max(abs(df$scanAngleRank)))
  return(r)
}
