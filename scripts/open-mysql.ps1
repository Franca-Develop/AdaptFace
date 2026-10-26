$container = "adaptface-main-mysql-1"
$database = "adaptface"
$user = "adaptface"

$containerEnvironment = docker inspect $container --format '{{range .Config.Env}}{{println .}}{{end}}'
$password = ($containerEnvironment | Where-Object { $_ -like "MYSQL_PASSWORD=*" }) -replace "^MYSQL_PASSWORD=", ""

if (-not $password) {
    throw "Senha do MySQL não encontrada no container."
}

docker exec -it -e "MYSQL_PWD=$password" $container mysql -u $user $database
