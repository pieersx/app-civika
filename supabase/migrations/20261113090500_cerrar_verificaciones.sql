-- Cierra el camino directo a `verificaciones`. El trigger reabrir_si_sigue_igual convierte tres filas
-- «sigue igual» en un cambio de estado; con INSERT/UPDATE directo, tres cuentas cualesquiera (sin haber
-- reportado ni apoyado el incidente) podían reabrir un incidente resuelto de otra entidad.
-- La única vía de escritura es la RPC verificar_resolucion, que exige ciudadano activo, incidente
-- resuelto y haber reportado o apoyado. Es security definer, así que no depende de estos permisos.

revoke insert, update on public.verificaciones from authenticated;

drop policy if exists verificaciones_insercion_propia on public.verificaciones;
drop policy if exists verificaciones_actualizacion_propia on public.verificaciones;
