// Cria a collection `rendas_mensais` no Appwrite (feature de congelamento de renda por mês).
//
// Uso:
//   APPWRITE_API_KEY=<sua_api_key_com_databases.write> node scripts/create-rendas-collection.mjs
//
// Requisitos: Node 18+ e a dependência node-appwrite (o script tenta usar a instalada
// ou via `npx`). Endpoint/projeto/database são os do projeto cost (já conhecidos).
//
// Ao final, imprime o COLLECTION ID — copie para `local.properties`:
//   appwrite.collectionRendas=<ID impresso>

import { Client, Databases, ID, IndexType, Permission, Role } from 'node-appwrite';

const ENDPOINT = process.env.APPWRITE_ENDPOINT ?? 'https://YOUR_APPWRITE_ENDPOINT/v1';
const PROJECT_ID = process.env.APPWRITE_PROJECT_ID ?? 'YOUR_PROJECT_ID';
const DATABASE_ID = process.env.APPWRITE_DATABASE_ID ?? 'YOUR_DATABASE_ID';
const API_KEY = process.env.APPWRITE_API_KEY;

if (!API_KEY) {
  console.error('ERRO: defina APPWRITE_API_KEY (API key do console com escopo databases.write).');
  process.exit(1);
}

const client = new Client().setEndpoint(ENDPOINT).setProject(PROJECT_ID).setKey(API_KEY);
const db = new Databases(client);

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function waitAttributesAvailable(databaseId, collectionId, keys) {
  for (let i = 0; i < 30; i++) {
    const list = await db.listAttributes(databaseId, collectionId);
    const ok = keys.every((k) => list.attributes.find((a) => a.key === k && a.status === 'available'));
    if (ok) return;
    await sleep(1000);
  }
  throw new Error('Timeout aguardando atributos ficarem disponíveis.');
}

async function main() {
  const permissions = [
    Permission.read(Role.users()),
    Permission.create(Role.users()),
    Permission.update(Role.users()),
    Permission.delete(Role.users()),
  ];

  const col = await db.createCollection(DATABASE_ID, ID.unique(), 'rendas_mensais', permissions);
  console.log('Collection criada:', col.$id);

  await db.createStringAttribute(DATABASE_ID, col.$id, 'groupId', 255, true);
  await db.createStringAttribute(DATABASE_ID, col.$id, 'competencia', 7, true);
  await db.createStringAttribute(DATABASE_ID, col.$id, 'pessoaId', 255, true);
  await db.createIntegerAttribute(DATABASE_ID, col.$id, 'rendaCentavos', true, 0);

  await waitAttributesAvailable(DATABASE_ID, col.$id, ['groupId', 'competencia', 'pessoaId', 'rendaCentavos']);

  await db.createIndex(
    DATABASE_ID,
    col.$id,
    'idx_group_competencia',
    IndexType.Key,
    ['groupId', 'competencia'],
    ['ASC', 'ASC'],
  );

  console.log('\n✅ Pronto! Adicione ao local.properties:');
  console.log(`appwrite.collectionRendas=${col.$id}`);
}

main().catch((e) => {
  console.error('Falhou:', e?.message ?? e);
  process.exit(1);
});
