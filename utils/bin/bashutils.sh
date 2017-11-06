#!/usr/bin/env bash

# ==============================================================================
# Miscellaneous useful functions to be used in a bash script.
# ==============================================================================


# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# Read-only boolean values.
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
declare -ir TRUE=0
declare -ir FALSE=1


# ------------------------------------------------------------------------------
# Checks if current user is root and aborts if it is not.
#
# STDERR: "Please run as root\n<aborted>\n" if current user is not root
#
# RETURN: $TRUE if, and only if, current user is root
#
# EXIT  : '1' if, and only if, current user is NOT root
# ------------------------------------------------------------------------------
assert_is_root() {
    is_root || abort "Please run as root"
}

# ------------------------------------------------------------------------------
# Checks if current user is root.
#
# RETURN: $TRUE if current user is root; $FALSE otherwise
# ------------------------------------------------------------------------------
is_root() {
    as_bool '[ $EUID -eq 0 ]'
}


# ------------------------------------------------------------------------------
# TODO: Document this function.
# ------------------------------------------------------------------------------
query() {
    local prefix='? '
    [ -n "$1" ] && prefix="$1 "

    local -i default
    case "x${2^^}" in
        xY|xYES )
            default=0
        ;;

        xN|xNO )
            default=1
        ;;
    esac

    local option
    while true; do
        printf "%s" "$prefix"
        option=
        IFS= read -r option
        case "x${option^^}" in
            xY|xYES )
                return 0
            ;;

            xN|xNO )
                return 1
            ;;

            x )
                [ -n $default ] && return $default
            ;;
        esac
        echo "Please answer (Y/N) or (yes/no)"
    done
}

# ------------------------------------------------------------------------------
# TODO: Document this function.
# ------------------------------------------------------------------------------
query_choice(){
    _CHOICE=
    _CHOICE_INDEX=

    local -a all_choices=("$@")
    local -i num_choices=${#all_choices[@]}
    local choice
    local -i choice_index
    local -i choice_number
    if [ $num_choices -gt 0 ]; then
        while true; do
            printf "\nPlease select one of the following:\n"
            for i in "${!all_choices[@]}"; do
                choice="${all_choices[$i]}"
                choice_index=$i
                choice_number=$choice_index+1
                printf "  %d)\t%s\n" "$choice_number" "$choice"
            done

            printf "\nInput choice number or text (or nothing to abort): "
            choice=
            IFS= read -r choice
            if [ -n "$choice" ]; then
                if is_integer "$choice"; then
                    choice_number=$choice
                    choice_index=$choice_number-1
                    if [ $choice_index -ge 0 -a $choice_index -lt $num_choices ]; then
                        _CHOICE="${all_choices[$choice_index]}"
                        _CHOICE_INDEX=$choice_index
                        return $TRUE
                    fi
                else
                    choice_index=$(index_of_ic "$choice" "${all_choices[@]}")
                    if [ $? -eq $TRUE ]; then
                        _CHOICE="${all_choices[$choice_index]}"
                        _CHOICE_INDEX=$choice_index
                        return $TRUE
                    fi
                fi
            else
                break
            fi

        echo "Invalid input"
        done
    fi

    return $FALSE
}

# ------------------------------------------------------------------------------
# Prints the provided arguments to the standard output, starting at the second
# argument, all joined by the separator character provided as the first
# argument.
#
# ARGS:
#   $1     - The separator character (required)
#   ${@:2} - The strings to be joined (optional)
#
# STDOUT: "${@:2}", all arguments joined by the first character of "$1"
# STDERR: "First argument is required in function 'join_by'" if "$1" is not
#         provided
#
# RETURN: The exit status of the 'printf' command
# EXIT  : If "$1" is not provided
# ------------------------------------------------------------------------------
join_by() {
    local -r IFS="${1?First argument is required in function 'join_by'}"
    printf "%s" "${*:2}"
}

# ------------------------------------------------------------------------------
# Prints to the standard output the position of the first argument in the
# (1-based) array containing the remaining arguments. If the array does not
# contain the first argument, then '-1' is printed. The argument comparison is
# an exact string match.
#
# ARGS:
#   $1     - The string to be searched (required)
#   ${@:2} - The (1-based) array of strings to search
#
# STDOUT: The position of "$1" in the array ("${@:2}"), or '-1' if the array
#         does not contain "$1"
# STDERR: "First argument is required in function 'index_of'" if "$1" is not
#         provided
#
# RETURN: $TRUE if "$1" is contained in the array ("${@:2}"); $FALSE otherwise
# EXIT  :  If "$1" is not provided
# ------------------------------------------------------------------------------
index_of() {
    local str="${1?First argument is required in function 'index_of'}"
    local -ar choices=("${@:2}")

    for i in "${!choices[@]}"; do
        if [ "$str" = "${choices[$i]}" ]; then
            printf "%d" "$i"
            return $TRUE
        fi
    done

    printf "%d" "-1"
    return $FALSE
}

# ------------------------------------------------------------------------------
# Prints to the standard output the position of the first argument in the
# (1-based) array containing the remaining arguments. If the array does not
# contain the first argument, then '-1' is printed. The argument comparison is
# a string match ignoring case.
#
# ARGS:
#   $1     - The string to be searched (required)
#   ${@:2} - The (1-based) array of strings to search
#
# STDOUT: The position of "$1" in the array ("${@:2}"), ignoring case, or '-1'
#         if the array does not contain "$1"
# STDERR: "First argument is required in function 'index_of_ic'" if "$1" is not
#         provided
#
# RETURN: $TRUE if "$1" is contained in the array ("${@:2}"), ignoring case;
#         $FALSE otherwise
# EXIT  :  If "$1" is not provided
#
index_of_ic() {
    local str="${1?First argument is required in function 'index_of_ic'}"
    local -ar choices=("${@:2}")

    for i in "${!choices[@]}"; do
        if equals_ignore_case "$str" "${choices[$i]}"; then
            printf "%d" "$i"
            return $TRUE
        fi
    done

    printf "%d" "-1"
    return $FALSE
}

# ------------------------------------------------------------------------------
# Checks if two provided strings are equal, ignoring case.
#
# ARGS:
#   $1 - The first string to be compared (optional)
#   $2 - The second string to be compared (optional)
#
# RETURN: $TRUE if "$1" and "$2" are equal string, ignoring case; $FALSE
#         otherwise
# ------------------------------------------------------------------------------
equals_ignore_case() {
    as_bool '[' $(quote "${1^^}") '=' $(quote "${2^^}") ']'
}

# ------------------------------------------------------------------------------
# Checks if the provided argument is a non-negative integer.
#
# ARGS:
#   $1 - String to check if it is a non-negative integer
#
# RETURN: $TRUE if "$1" is a non-negative integer; $FALSE otherwise
# ------------------------------------------------------------------------------
is_integer() {
    case "$1" in
        ''|*[!0-9]* )
            return $FALSE ;;
        * )
            return $TRUE ;;
    esac
}

# ------------------------------------------------------------------------------
# Evaluates the provided command and returns its exit status converted to a
# boolean value ($TRUE or $FALSE). If no command is provided, returns $TRUE.
#
# ARGS  : Command to be evaluated (optional)
#
# RETURN: $TRUE if command is null or exits with status '0'; $FALSE otherwise
# ------------------------------------------------------------------------------
as_bool() {
    eval "$@" && return $TRUE || return $FALSE
}

# ------------------------------------------------------------------------------
# Prints to the standard output the provided arguments, each inside single
# quotes. More specifically, each argument is quoted according to the 'quote'
# function.
#
# ARGS  : Strings to be quoted (optional)
#
# STDOUT: "$*", each argument quoted according to the 'quote' function
#
# RETURN: The exit status of the 'printf' command
# ------------------------------------------------------------------------------
quote_args() {
    local -a quoted=()
    for arg in "$@"; do
        quoted+=("$(quote "$arg")")
    done

    printf "%s" "${quoted[*]}"
}

# ------------------------------------------------------------------------------
# Prints to the standard output the input string inside single quotes. If the
# input itself contains single quotes, then the input is first split around
# those quotes and then re-concatenated back with escaped single quotes between
# the split parts.
#
# ARGS:
#   $1 - A string to be quoted (optional)
#
# STDOUT: "'" + "${1//\'/\'\\\'\'}" + "'"
#
# RETURN: The exit status of the 'printf' command
# ------------------------------------------------------------------------------
quote() {
    local sq_escaped="${1//\'/\'\\\'\'}"
    printf "'%s'" "$sq_escaped"
}

# ------------------------------------------------------------------------------
# Exits with status '0' after printing to the standard output an optional line
# passed in the provided arguments followed by the line '<done>'.
#
# ARGS  : Line to be printed (optional)
#
# STDOUT: An optional line followed by "<done>\n"
#
# EXIT  : '0'
# ------------------------------------------------------------------------------
conclude() {
    print_msg "$@"
    print_msg "<done>"
    exit 0
}

# ------------------------------------------------------------------------------
# Exits with status '1' after printing to the standard error an optional line
# passed in the provided arguments followed by the line '<aborted>'.
#
# ARGS  : Line to be printed (optional)
#
# STDERR: An optional line followed by "<aborted>\n"
#
# EXIT  : '1'
# ------------------------------------------------------------------------------
abort() {
    print_error "$@"
    print_error "<aborted>"
    exit 1
}

# ------------------------------------------------------------------------------
# Prints to the standard error the provided arguments followed by a newline
# character. If no arguments are provided, nothing is printed.
#
# ARGS  : Line to be printed (optional)
#
# STDERR: "$*" followed by "\n", only if some arguments are provided
#
# RETURN: the exit status of the 'printf' command
# ------------------------------------------------------------------------------
print_error() {
    print_msg "$@" >&2
}

# ------------------------------------------------------------------------------
# Prints to the standard output the provided arguments followed by a newline
# character. If no arguments are provided, nothing is printed.
#
# ARGS  : Line to be printed (optional)
#
# STDOUT: "$*" followed by "\n", only if some arguments are provided
#
# RETURN: the exit status of the 'printf' command
# ------------------------------------------------------------------------------
print_msg() {
    if [ $# -gt 0 ]; then
        printf "%s\n" "$*"
    fi
}

