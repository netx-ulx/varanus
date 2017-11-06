#!/usr/bin/env bash

source "$(dirname "$0")/bashutils.sh"

declare -r LOG_DIR="/tmp"
declare -r LOG_EXT="log"

declare -r PID_DIR="/tmp"
declare -r PID_EXT="pid"


run() {
    local operation; operation="$(get_operation "$1")"
    [ $? -eq $TRUE ] || abort

    local execfile; execfile="$(get_execfile "$2")"
    [ $? -eq $TRUE ] || abort

    $operation "$execfile" "${@:3}"
}


get_operation() {
    local op="$1"
    if [ -n "$op" ]; then
        case "${op,,}" in
            start)  printf "do_start"
                    return $TRUE
            ;;

            stop)   printf "do_stop"
                    return $TRUE
            ;;

            view)   printf "do_view"
                    return $TRUE
            ;;

            *)      print_error "ERROR: Invalid operation, use start|stop|view"
                    return $FALSE
            ;;
        esac
    else
        print_error "ERROR: Must specify an operation (start|stop|view)"
        return $FALSE
    fi
}


get_execfile() {
    local file="$1"
    if [ -n "$file" ]; then
        file="$(readlink -f "$file")"
        if [ -f "$file" -a -x "$file" ]; then
            printf "%s" "$file"
            return $TRUE
        else
            print_error "ERROR: Cannot execute file passed as argument"
            return $FALSE
        fi
    else
        print_error "ERROR: Must pass at least the path of an executable file"
        return $FALSE
    fi
}


do_start() {
    local logfile="$(build_aux_file_name "$LOG_DIR" "$LOG_EXT" "$@")"
    local pidfile="$(build_aux_file_name "$PID_DIR" "$PID_EXT" "$@")"
    local cmd="$(build_command "$@")"

    eval "$cmd > '$logfile' 2>&1 &"
    local -i pid="$!"
    printf "%d" "$pid" > "$pidfile"
    echo "<program started (PID = $pid)>"
}


do_stop() {
    local pidfile="$(build_aux_file_name "$PID_DIR" "$PID_EXT" "$@")"

    if [ -f "$pidfile" -a -r "$pidfile" ]; then
        local -i pid="$(< $pidfile)"
        if pkill -TERM -P "$pid"; then
            rm -f "$pidfile"
            echo "<program stopped (PID = $pid)>"
        else
            abort "ERROR: Could not stop program (maybe it isn't running?)"
        fi
    else
        abort "ERROR: Could not find PID file to stop program (maybe it isn't running?)"
    fi
}


do_view(){
    local logfile="$(build_aux_file_name "$LOG_DIR" "$LOG_EXT" "$@")"

    if [ -f "$logfile" -a -r "$logfile" ]; then
        less -r +F "$logfile"
    else
        abort "ERROR: Background log file not found (maybe program isn't running?)"
    fi
}


build_aux_file_name() {
    local dir="$1"
    local ext="$2"
    local prog_as_fname="$(path_to_filename "$3")"
    local args_as_fname="$(join_by _ "${@:4}")"

    if [ -n "$args_as_fname" ]; then
        printf "%s/%s_%s.%s" "$dir" "$prog_as_fname" "$args_as_fname" "$ext"
    else
        printf "%s/%s.%s" "$dir" "$prog_as_fname" "$ext"
    fi
}


path_to_filename() {
    local path="$1"
    if [ -n "$path" ]; then
        printf "%s" "${path//'/'/'_'}"
    fi
}


build_command() {
    local prog="$1"
    local args=("${@:2}")

    if [ ${#args[@]} -gt 0 ]; then
        printf "%s %s" "$prog" "$(quote_args "${args[@]}")"
    else
        printf "%s" "$prog"
    fi
}


run "$@"

